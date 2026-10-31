package dev.vortex.launcher;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Vortex Launcher (beta): Microsoft login, Modrinth mod browser, optimizer pack, configurable background. */
public class LauncherApp extends Application {
    private static final Map<String, String[]> THEMES = Map.of(
            "Vortex", new String[]{"#0e0e14", "#16161f", "#8b5cf6", "#3b82f6"},
            "Ember", new String[]{"#120d0d", "#1c1414", "#f97316", "#ef4444"},
            "Mint", new String[]{"#0b1210", "#121c19", "#10b981", "#06b6d4"},
            "Mono", new String[]{"#0c0c0c", "#161616", "#e5e5e5", "#9ca3af"});
    private static final List<String> BACKGROUNDS = List.of("Aurora", "Gradient", "Solid", "Custom");

    private final LauncherConfig cfg = LauncherConfig.load();
    private final ModrinthClient modrinth = new ModrinthClient();
    private MicrosoftAuth.Account account;

    private StackPane root;
    private BorderPane shell;
    private Label accountLabel;
    private final TextArea log = new TextArea();
    private double t;

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        shell = new BorderPane();
        shell.setLeft(sidebar());
        shell.setCenter(accountPage());
        root.getChildren().add(shell);
        applyStyle();
        log.setEditable(false);
        log.setPrefRowCount(6);
        stage.setTitle("Vortex Launcher");
        stage.setScene(new Scene(root, 940, 620));
        stage.show();
        trySilentLogin();
    }

    // ---------- styling / background ----------
    private void applyStyle() {
        String[] c = THEMES.getOrDefault(cfg.theme, THEMES.get("Vortex"));
        String bg;
        switch (cfg.background) {
            case "Gradient" -> bg = "-fx-background-color: linear-gradient(to bottom right, " + c[0] + ", " + c[2] + "33, " + c[3] + "22);";
            case "Solid" -> bg = "-fx-background-color: " + c[0] + ";";
            case "Custom" -> bg = customBg(c);
            default -> bg = "-fx-background-color: radial-gradient(center 20% 20%, radius 70%, " + c[2] + "55, " + c[0] + "),"
                    + " radial-gradient(center 85% 80%, radius 60%, " + c[3] + "44, transparent);";
        }
        root.setStyle(bg + " -fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-text-fill: white;");
        String css = """
                .root { -fx-base: %s; }
                .label { -fx-text-fill: #f1f1f7; }
                .dim { -fx-text-fill: #8a8aa0; }
                .button { -fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand; }
                .button:hover { -fx-background-color: %s; }
                .accent { -fx-background-color: linear-gradient(to right, %s, %s); }
                .text-field, .text-area, .combo-box, .list-view { -fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 8; -fx-control-inner-background: %s; -fx-border-radius: 8; }
                .list-cell { -fx-background-color: transparent; -fx-text-fill: white; }
                .list-cell:selected { -fx-background-color: %s55; }
                """.formatted(c[1], c[1], "#2a2a3a", c[2], c[3], c[1], c[1], c[2]);
        shell.getStylesheets().clear();
        shell.getStylesheets().add("data:text/css," + css.replace("\n", " ").replace("%", "%25").replace("#", "%23").replace(" ", "%20"));
    }

    private String customBg(String[] c) {
        try {
            File f = new File(cfg.customBackground);
            if (f.isFile()) {
                Image img = new Image(f.toURI().toString(), 0, 0, true, true, true);
                if (!img.isError()) {
                    return "-fx-background-image: url('" + f.toURI() + "'); -fx-background-size: cover; -fx-background-position: center;";
                }
            }
        } catch (Exception ignored) {}
        return "-fx-background-color: " + c[0] + ";";
    }

    // ---------- shell ----------
    private VBox sidebar() {
        Canvas logo = new Canvas(64, 64);
        new AnimationTimer() {
            @Override public void handle(long now) { t = now / 1e9; drawLogo(logo.getGraphicsContext2D()); }
        }.start();
        Label name = new Label("VORTEX");
        name.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-letter-spacing: 4;");
        Label sub = new Label("Launcher beta");
        sub.getStyleClass().add("dim");
        accountLabel = new Label("Not signed in");
        accountLabel.getStyleClass().add("dim");
        Button b1 = nav("Account", () -> shell.setCenter(accountPage()));
        Button b2 = nav("Mods", () -> shell.setCenter(modsPage()));
        Button b3 = nav("Appearance", () -> shell.setCenter(appearancePage()));
        VBox box = new VBox(10, logo, name, sub, new Separator(), b1, b2, b3, new Region(), accountLabel);
        VBox.setVgrow(box.getChildren().get(7), Priority.ALWAYS);
        box.setPadding(new Insets(20));
        box.setPrefWidth(190);
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-background-color: rgba(0,0,0,0.35);");
        return box;
    }

    private Button nav(String text, Runnable r) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> r.run());
        return b;
    }

    private void drawLogo(GraphicsContext g) {
        String[] c = THEMES.getOrDefault(cfg.theme, THEMES.get("Vortex"));
        g.clearRect(0, 0, 64, 64);
        for (int arm = 0; arm < 2; arm++) {
            g.setFill(Color.web(arm == 0 ? c[2] : c[3]));
            for (int i = 0; i < 46; i++) {
                double f = i / 46.0, ang = f * 4.6 + arm * Math.PI + t * 0.9, r = f * 30;
                double s = 1 + f * 4;
                g.setGlobalAlpha(0.25 + 0.75 * f);
                g.fillOval(32 + Math.cos(ang) * r - s / 2, 32 + Math.sin(ang) * r - s / 2, s, s);
            }
        }
        g.setGlobalAlpha(1);
        g.setFill(Color.WHITE);
        g.fillOval(29, 29, 6, 6);
    }

    // ---------- pages ----------
    private VBox page(String title, String subtitle) {
        Label h = new Label(title);
        h.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label s = new Label(subtitle);
        s.getStyleClass().add("dim");
        s.setWrapText(true);
        VBox v = new VBox(12, h, s);
        v.setPadding(new Insets(28));
        return v;
    }

    private VBox accountPage() {
        VBox v = page("Account", "Sign in with Microsoft to use your Minecraft account. No password is entered here: you approve a one-time code on microsoft.com.");
        Label info = new Label(account == null ? "Not signed in." : "Signed in as " + account.name());
        Button login = new Button("Sign in with Microsoft");
        login.getStyleClass().add("accent");
        Button out = new Button("Sign out");
        Label code = new Label();
        code.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-letter-spacing: 6;");
        Label status = new Label();
        status.getStyleClass().add("dim");
        status.setWrapText(true);
        login.setOnAction(e -> startLogin(login, code, status, info));
        out.setOnAction(e -> {
            MicrosoftAuth.signOut();
            account = null;
            accountLabel.setText("Not signed in");
            info.setText("Not signed in.");
        });
        v.getChildren().addAll(info, new HBox(10, login, out), code, status);
        return v;
    }

    private void startLogin(Button login, Label code, Label status, Label info) {
        String id = cfg.effectiveClientId();
        if (id.isBlank()) {
            status.setText("Set your Azure client ID first (Appearance > Launcher settings, or env VORTEX_MS_CLIENT_ID). See docs/ARCHITECTURE.md.");
            return;
        }
        login.setDisable(true);
        MicrosoftAuth auth = new MicrosoftAuth(id);
        Task<MicrosoftAuth.Account> task = new Task<>() {
            @Override protected MicrosoftAuth.Account call() throws Exception {
                MicrosoftAuth.DeviceCode dc = auth.requestCode();
                Platform.runLater(() -> {
                    code.setText(dc.userCode());
                    status.setText("Open " + dc.verificationUri() + " and enter this code. (Code copied to clipboard.)");
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(dc.userCode());
                    Clipboard.getSystemClipboard().setContent(cc);
                    openBrowser(dc.verificationUri());
                });
                return auth.completeLogin(dc, s -> Platform.runLater(() -> status.setText(s)));
            }
        };
        task.setOnSucceeded(e -> {
            account = task.getValue();
            code.setText("");
            status.setText("Done.");
            info.setText("Signed in as " + account.name());
            accountLabel.setText(account.name());
            login.setDisable(false);
        });
        task.setOnFailed(e -> {
            status.setText(String.valueOf(task.getException().getMessage()));
            code.setText("");
            login.setDisable(false);
        });
        daemon(task);
    }

    private void trySilentLogin() {
        String id = cfg.effectiveClientId();
        if (id.isBlank()) return;
        Task<MicrosoftAuth.Account> task = new Task<>() {
            @Override protected MicrosoftAuth.Account call() { return new MicrosoftAuth(id).trySilent(s -> {}); }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue() != null) {
                account = task.getValue();
                accountLabel.setText(account.name());
            }
        });
        daemon(task);
    }

    private VBox modsPage() {
        VBox v = page("Mods", "Fabric mods for Minecraft " + cfg.mcVersion + " from Modrinth. Required dependencies install automatically; every file is checksum-verified.");
        TextField q = new TextField();
        q.setPromptText("Search mods (empty = most downloaded)");
        Button go = new Button("Search");
        go.getStyleClass().add("accent");
        Button pack = new Button("Install Optimizer Pack");
        pack.getStyleClass().add("accent");
        Button open = new Button("Open mods folder");
        ListView<ModrinthClient.Hit> list = new ListView<>();
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ModrinthClient.Hit h, boolean empty) {
                super.updateItem(h, empty);
                setText(empty || h == null ? null : h.title() + "  -  " + h.description() + "  (" + h.downloads() + " downloads)");
            }
        });
        Button install = new Button("Install selected");
        VBox.setVgrow(list, Priority.ALWAYS);

        Runnable search = () -> {
            Task<List<ModrinthClient.Hit>> task = new Task<>() {
                @Override protected List<ModrinthClient.Hit> call() throws Exception { return modrinth.search(q.getText().trim(), cfg.mcVersion, 30); }
            };
            task.setOnSucceeded(e -> list.getItems().setAll(task.getValue()));
            task.setOnFailed(e -> note("Search failed: " + task.getException().getMessage()));
            daemon(task);
        };
        go.setOnAction(e -> search.run());
        q.setOnAction(e -> search.run());
        install.setOnAction(e -> {
            var sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) installAll(List.of(sel.slug()));
        });
        pack.setOnAction(e -> installAll(ModrinthClient.OPTIMIZER_PACK));
        open.setOnAction(e -> {
            try { java.nio.file.Files.createDirectories(cfg.modsDir()); Desktop.getDesktop().open(cfg.modsDir().toFile()); }
            catch (Exception ex) { note("Could not open folder: " + ex.getMessage()); }
        });
        v.getChildren().addAll(new HBox(8, q, go), new HBox(8, install, pack, open), list, log);
        HBox.setHgrow(q, Priority.ALWAYS);
        search.run();
        return v;
    }

    private void installAll(List<String> slugs) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                for (String s : slugs) {
                    modrinth.install(s, cfg.mcVersion, cfg.modsDir(), m -> Platform.runLater(() -> note(m)));
                }
                Platform.runLater(() -> note("Finished. Mods are in " + cfg.modsDir()));
                return null;
            }
        };
        task.setOnFailed(e -> note("Install failed: " + task.getException().getMessage()));
        daemon(task);
    }

    private VBox appearancePage() {
        VBox v = page("Appearance", "Theme and background for the launcher.");
        ComboBox<String> theme = new ComboBox<>();
        theme.getItems().addAll(THEMES.keySet());
        theme.setValue(cfg.theme);
        ComboBox<String> bg = new ComboBox<>();
        bg.getItems().addAll(BACKGROUNDS);
        bg.setValue(cfg.background);
        TextField img = new TextField(cfg.customBackground);
        img.setPromptText("Path to a custom background image (png/jpg)");
        Button browse = new Button("Browse...");
        browse.setOnAction(e -> {
            var fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(root.getScene().getWindow());
            if (f != null) img.setText(f.getAbsolutePath());
        });
        TextField mc = new TextField(cfg.mcVersion);
        TextField cid = new TextField(cfg.clientId);
        cid.setPromptText("Azure application (client) ID");
        Button apply = new Button("Apply & save");
        apply.getStyleClass().add("accent");
        apply.setOnAction(e -> {
            cfg.theme = theme.getValue();
            cfg.background = bg.getValue();
            cfg.customBackground = img.getText().trim();
            if (mc.getText().trim().matches("[0-9a-zA-Z._-]{1,20}")) cfg.mcVersion = mc.getText().trim();
            cfg.clientId = cid.getText().trim();
            cfg.save();
            applyStyle();
        });
        v.getChildren().addAll(new Label("Theme"), theme, new Label("Background"), bg, new Label("Custom image"), new HBox(8, img, browse),
                new Separator(), new Label("Launcher settings"), new Label("Minecraft version"), mc, new Label("Microsoft (Azure) client ID"), cid, apply);
        HBox.setHgrow(img, Priority.ALWAYS);
        return v;
    }

    // ---------- helpers ----------
    private void note(String s) { log.appendText(s + "\n"); }

    private static void daemon(Task<?> t) {
        Thread th = new Thread(t, "vortex-task");
        th.setDaemon(true);
        th.start();
    }

    private static void openBrowser(String uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(uri));
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) { launch(args); }
}
