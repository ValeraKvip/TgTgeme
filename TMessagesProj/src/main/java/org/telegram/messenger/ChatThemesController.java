package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatThemesController extends BaseController implements NotificationCenter.NotificationCenterDelegate {
    private HashMap<String, TLRPC.TL_chatTheme> themes = new HashMap<>();
    private HashMap<String, ThemePreview> installed_themes = new HashMap<>();
    private HashMap<String, LoadingInfo> load_themes = new HashMap<>();
    private long loadDate;
    private int loadHash;

    private static volatile ChatThemesController[] Instance = new ChatThemesController[UserConfig.MAX_ACCOUNT_COUNT];


    public static ChatThemesController getInstance(int num) {
        ChatThemesController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (ChatThemesController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new ChatThemesController(num);
                }
            }
        }
        return localInstance;
    }


    public ChatThemesController(int num) {
        super(num);

        AndroidUtilities.runOnUIThread(() -> {
            MessagesController messagesController = getMessagesController();
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileLoaded);
            getNotificationCenter().addObserver(messagesController, NotificationCenter.fileLoadFailed);
        });
    }

    public HashMap<String, ThemePreview> getThemes() {
        checkThemes();
        return installed_themes;
    }

    public void checkThemes() {
        if (Math.abs(System.currentTimeMillis() / 1000 - loadDate) >= 60 * 60) {
            fetchThemes();
        }
    }

    private void fetchThemes() {
        TLRPC.TL_account_getChatThemes req = new TLRPC.TL_account_getChatThemes();
        req.hash = loadHash;

        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_account_chatThemes) {
                ArrayList<TLRPC.TL_chatTheme> themeList = ((TLRPC.TL_account_chatThemes) response).themes;
                themes.clear();
                for (int i = 0; i < themeList.size(); ++i) {
                    themes.put(themeList.get(i).emoticon, themeList.get(i));
                }
                loadDate = System.currentTimeMillis();
                loadHash = ((TLRPC.TL_account_chatThemes) response).hash;
                loadThemes();
            } else if (response instanceof TLRPC.TL_account_chatThemesNotModified) {
                loadDate = System.currentTimeMillis();
            }
        });
    }

    class LoadingInfo {
        public Theme.ThemeInfo loadingThemeInfo;
        public Theme.ThemeInfo light;
        public Theme.ThemeInfo dark;
        public TLRPC.TL_theme loadingTheme;
        public TLRPC.TL_wallPaper loadingThemeWallpaper;
        public String loadingThemeWallpaperName;
        public  boolean isDark;
        public String emoji;
    }

    private void loadThemes() {
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
        load_themes.clear();
        installed_themes.clear();

        for (Map.Entry<String, TLRPC.TL_chatTheme> entry : themes.entrySet()) {
            TLRPC.TL_chatTheme theme = entry.getValue();
            if (theme == null) {
                continue;
            }


            TLRPC.TL_theme themeLight = (TLRPC.TL_theme) theme.theme;
            TLRPC.TL_theme themeDark = (TLRPC.TL_theme) theme.dark_theme;

            TLRPC.TL_theme arr[] = new TLRPC.TL_theme[2];
            arr[0] = themeLight;
            arr[1] = themeDark;

            for (int i = 0; i < 1; ++i) {
                LoadingInfo loadingInfo = new LoadingInfo();

                loadingInfo.emoji = theme.emoticon;


                TLRPC.TL_theme t = themeDark;//arr[i];

                if (t.settings != null) {
                    loadingInfo.light =  Theme.getThemeTemplate();
                    loadingInfo.light.info = themeLight;
                    int accentId = loadingInfo.light.createNewAccent(loadingInfo.light.info, UserConfig.selectedAccount).id;
                    loadingInfo.light.setCurrentAccentId(accentId);

                    loadingInfo.dark = Theme.getThemeTemplate();
                    loadingInfo.dark.info = themeDark;

                     accentId = loadingInfo.dark.createNewAccent(loadingInfo.dark.info, UserConfig.selectedAccount).id;
                    loadingInfo.dark.setCurrentAccentId(accentId);

                    String key = Theme.getBaseThemeKey(t.settings);
                    Theme.ThemeInfo info = Theme.getTheme(key);
                    info.info = t;
                    loadingInfo.loadingThemeInfo = info;

                    if (info != null) {
                        TLRPC.TL_wallPaper object;

                        if (t.settings.wallpaper instanceof TLRPC.TL_wallPaper) {
                            object = (TLRPC.TL_wallPaper) t.settings.wallpaper;
                            File path = FileLoader.getPathToAttach(object.document, true);
                            //TODO hz have to be set without downloading
                            loadingInfo.loadingTheme = t;
                            loadingInfo.loadingThemeWallpaper = object;
                            loadingInfo.loadingThemeWallpaperName = FileLoader.getAttachFileName(object.document);
                            load_themes.put(loadingInfo.loadingThemeWallpaperName, loadingInfo);

                           // if (!path.exists()) {



                                FileLoader.getInstance(currentAccount).loadFile(object.document, object, 1, 1);
//                            }
//                            else{
//                                finishLoading(path.getAbsolutePath(),path);
//                            }
                        }

                    }
                }
            }
        }
    }

    public void cleanup() {
        loadDate = 0;
        loadHash = 0;
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
    }

    public static class ThemePreview {
        public File file;
        public Theme.ThemeInfo lightTheme;
        public Theme.ThemeInfo darkTheme;
        public String emoticon;


        public ThemePreview(String emoji) {
            this.emoticon = emoji;
        }
    }

    void finishLoading(String path, File file){
        if (load_themes.containsKey(path)) {
            LoadingInfo loadingInfo = load_themes.remove(path);
            Theme.ThemeInfo info = loadingInfo.loadingThemeInfo;
            Utilities.globalQueue.postRunnable(() -> {
                info.pathToWallpaper = path;

                AndroidUtilities.runOnUIThread(() -> {
                    if (loadingInfo.loadingTheme == null) {
                        return;
                    }

                    ThemePreview preview;
                    if(installed_themes.containsKey(loadingInfo.emoji)){
                        preview = installed_themes.get(loadingInfo.emoji);
                    }
                    else{
                        preview = new ThemePreview(loadingInfo.emoji);
                    }

                    preview.file = file;
                    preview.darkTheme = loadingInfo.dark;
                    preview.lightTheme =loadingInfo.light;
                    installed_themes.put(preview.emoticon, preview);
                });
            });

        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoaded) {
            String path = (String) args[0];
            File file = (File) args[1];
            finishLoading(path,file);
        }
    }
}
