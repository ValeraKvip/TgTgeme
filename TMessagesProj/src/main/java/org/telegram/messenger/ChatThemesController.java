package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatThemesController extends BaseController {
    private final static String config = "chat_theme_config";
    private static final String THEMES_COUNT = "THEMES_COUNT";
    private static final String CHAT_THEME = "CHAT_THEME";
    private static final String THEME_HASH = "THEME_HASH";

    private HashMap<String, ThemePreview> installed_themes = new HashMap<>();

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
    }

    public HashMap<String, ThemePreview> getThemes() {
        checkThemes();
        return installed_themes;
    }


    public void checkThemes() {
        if (Math.abs(System.currentTimeMillis() - loadDate) >= 60 * 60 * 1000) {
            fetchThemes();
        } else if (installed_themes.size() == 0) {
            loadThemes(getFromCache(), true);
        }
    }

    private ArrayList<TLRPC.TL_chatTheme> getFromCache() {
        SharedPreferences prefs = ApplicationLoader.applicationContext
                .getSharedPreferences(config, Context.MODE_PRIVATE);

        int count = prefs.getInt(THEMES_COUNT, 0);
        ArrayList<TLRPC.TL_chatTheme> themes = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            try {
                String val = prefs.getString(CHAT_THEME + i, "");
                if (val.isEmpty()) {
                    continue;
                }

                SerializedData data = new SerializedData(Utilities.hexToBytes(val));
                TLRPC.TL_chatTheme theme = TLRPC.TL_chatTheme
                        .TLdeserialize(data, data.readInt32(true), true);

                if (theme != null) {
                    themes.add(theme);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        return themes;
    }

    private void fetchThemes() {
        SharedPreferences prefs = ApplicationLoader.applicationContext
                .getSharedPreferences(config, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (loadHash == 0) {
            loadHash = prefs.getInt(THEME_HASH, 0);
        }

        TLRPC.TL_account_getChatThemes req = new TLRPC.TL_account_getChatThemes();
        req.hash = loadHash;

        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_account_chatThemes) {
                ArrayList<TLRPC.TL_chatTheme> themeList = ((TLRPC.TL_account_chatThemes) response).themes;
                editor.clear();
                for (int i = 0; i < themeList.size(); ++i) {
                    try {
                        TLRPC.TL_chatTheme theme = themeList.get(i);
                        SerializedData data = new SerializedData(theme.getObjectSize());
                        theme.serializeToStream(data);
                        editor.putString(CHAT_THEME + i, Utilities.bytesToHex(data.toByteArray()));
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }

                loadDate = System.currentTimeMillis();
                loadHash = ((TLRPC.TL_account_chatThemes) response).hash;
                editor.putInt(THEME_HASH, loadHash);
                editor.putInt(THEMES_COUNT, themeList.size());
                editor.commit();

                loadThemes(themeList, false);
            } else if (response instanceof TLRPC.TL_account_chatThemesNotModified) {
                loadDate = System.currentTimeMillis();
                if (installed_themes.size() == 0) {
                    loadThemes(getFromCache(), true);
                }
            }
        });
    }

    private void loadThemes(List<TLRPC.TL_chatTheme> themes, boolean cached) {
        Utilities.globalQueue.postRunnable(() -> {
            installed_themes.clear();

            List<Theme.ThemeInfo> th = new ArrayList<>();
            for (TLRPC.TL_chatTheme theme : themes) {
                TLRPC.TL_theme themeLight = (TLRPC.TL_theme) theme.theme;
                TLRPC.TL_theme themeDark = (TLRPC.TL_theme) theme.dark_theme;

                ThemePreview preview = new ThemePreview(theme.emoticon);

                preview.darkTheme = Theme.ThemeInfo.createFromTL_theme(themeDark);
                preview.lightTheme = Theme.ThemeInfo.createFromTL_theme(themeLight);
                installed_themes.put(theme.emoticon, preview);
                th.add(preview.darkTheme);
                th.add(preview.lightTheme);
            }


            if (!cached) {
                Theme.PatternsLoader.createLoaderForChatThemes(th, true);
            }

        });
    }


    public void cleanup() {
        loadDate = 0;
        loadHash = 0;
        installed_themes.clear();
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

}
