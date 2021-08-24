package org.telegram.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatThemesController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ThemePickerView extends LinearLayout {

    private final RecyclerListView themeListView;
    private final TextView title;
    private final Button doneBtn;
    private ThemeAdapter adapter;
    private Drawable bgDrawable;
    private Theme.ThemeInfo fallbackTheme;
    private RLottieImageView darkThemeView;
    private RLottieDrawable sunDrawable;
    private Integer currentMoonColor;
    private int darkThemeBackgroundColor;
    private String selectedEmoticon;
    private ChatActivity chatActivity;
    private HashMap<String, ChatThemesController.ThemePreview> themes;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
         super.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        bgDrawable.setColorFilter(Theme.getColor(Theme.key_dialogBackground), PorterDuff.Mode.MULTIPLY);
        this.setBackground(bgDrawable);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));

//        int color = Theme.getColor(Theme.key_dialogTextBlack);
//
//        if (sunDrawable != null) {
//            sunDrawable.beginApplyLayerColors();
//            sunDrawable.setLayerColor("Sunny.**", color);
//            sunDrawable.setLayerColor("Path 6.**", color);
//            sunDrawable.setLayerColor("Path.**", color);
//            sunDrawable.setLayerColor("Path 5.**", color);
//            sunDrawable.commitApplyLayerColors();
//        }

        doneBtn.setBackgroundColor(Theme.getColor(Theme.key_dialogButton));
        doneBtn.setTextColor(Theme.getColor(Theme.key_chat_botButtonText));

    }

    public ThemePickerView(@NonNull Context context, ChatActivity ca) {
        super(context);
        this.chatActivity = ca;
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        fallbackTheme = Theme.getCurrentTheme();
        selectedEmoticon = chatActivity.selectedTheme;
        int m = AndroidUtilities.dp(10);
        this.setOrientation(VERTICAL);
        adapter = new ThemeAdapter();

        Rect paddings = new Rect();
        bgDrawable = context.getResources().getDrawable(R.drawable.sheet_shadow_round).mutate();
        bgDrawable.getPadding(paddings);
        bgDrawable.setColorFilter(new PorterDuffColorFilter(
                Theme.getColor(Theme.key_dialogBackground), PorterDuff.Mode.MULTIPLY));

        this.setBackground(bgDrawable);

        //setup text
        title = new TextView(context);
        LinearLayout.LayoutParams textParams = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT);
        textParams.gravity = Gravity.LEFT | Gravity.CENTER;
        textParams.weight = 1;
        title.setLayoutParams(textParams);
        title.setText(LocaleController.getString("SelectTheme", R.string.SelectTheme));


        title.setTextSize(20);

        sunDrawable = new RLottieDrawable(R.raw.sun, "" + R.raw.sun, AndroidUtilities.dp(28), AndroidUtilities.dp(28), true, null);
        if (!adapter.isDarkMode) {
            sunDrawable.setCustomEndFrame(36);
        } else {
            sunDrawable.setCustomEndFrame(0);
            sunDrawable.setCurrentFrame(36);
        }

        sunDrawable.setPlayInDirectionOfCustomEndFrame(true);
        darkThemeView = new RLottieImageView(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                if (sunDrawable.getCustomEndFrame() != 0) {
                    info.setText(LocaleController.getString("AccDescrSwitchToNightTheme", R.string.AccDescrSwitchToNightTheme));
                } else {
                    info.setText(LocaleController.getString("AccDescrSwitchToDayTheme", R.string.AccDescrSwitchToDayTheme));
                }
            }
        };

        ImageView dayNight = new ImageView(getContext());
        dayNight.setLayoutParams(LayoutHelper.createLinear(AndroidUtilities.dp(10),
                AndroidUtilities.dp(10),Gravity.RIGHT | Gravity.CENTER));

        dayNight.setImageDrawable(getResources().getDrawable(R.drawable.moon));
        dayNight.setColorFilter(Color.BLUE);
        dayNight.setOnClickListener(v -> {
//            RotateAnimation rotateAnimation = new RotateAnimation(0,180,
//                    AndroidUtilities.dp(5),AndroidUtilities.dp(5));
//            rotateAnimation.setDuration(500);
//            rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
//                @Override
//                public void onAnimationStart(Animation animation) {
//
//                }
//
//                @Override
//                public void onAnimationEnd(Animation animation) {
//                    if(adapter.isDarkMode){
//                        dayNight.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_wb_sunny_24));
//                    }else{
//                        dayNight.setImageDrawable(getResources().getDrawable(R.drawable.moon));
//                    }
//                }
//
//                @Override
//                public void onAnimationRepeat(Animation animation) {
//
//                }
//            });

            if (adapter != null) {
                adapter.switchMode();

                darkThemeView.setScaleType(ImageView.ScaleType.CENTER);
                darkThemeView.setAnimation(sunDrawable);

            }

            if(adapter.isDarkMode){
                dayNight.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_wb_sunny_24));
            }else{
                dayNight.setImageDrawable(getResources().getDrawable(R.drawable.moon));
            }
         //   dayNight.startAnimation(rotateAnimation);
        });

        int color = Color.BLACK;
        sunDrawable.beginApplyLayerColors();
        sunDrawable.setLayerColor("Sunny.**", color);
        sunDrawable.setLayerColor("Path 6.**", color);
        sunDrawable.setLayerColor("Path.**", color);
        sunDrawable.setLayerColor("Path 5.**", color);
        sunDrawable.commitApplyLayerColors();
       // darkThemeView.setImageDrawable(R.drawable.moon);
        darkThemeView.setScaleType(ImageView.ScaleType.CENTER);
        darkThemeView.setAnimation(sunDrawable);
        if (Build.VERSION.SDK_INT >= 21) {
            darkThemeView.setBackgroundDrawable(Theme.createSelectorDrawable(darkThemeBackgroundColor = Theme.getColor(Theme.key_listSelector), 1, AndroidUtilities.dp(17)));
            Theme.setRippleDrawableForceSoftware((RippleDrawable) darkThemeView.getBackground());
        }
//        darkThemeView.setOnClickListener(v -> {
//            if (adapter != null) {
//                adapter.switchMode();
//
//                darkThemeView.setScaleType(ImageView.ScaleType.CENTER);
//                darkThemeView.setAnimation(sunDrawable);
//            }
//        });


        darkThemeView.setLayoutParams(LayoutHelper.createLinear(1,1, Gravity.RIGHT | Gravity.CENTER));

        doneBtn = new Button(getContext());
        doneBtn.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        doneBtn.setText("Done");

        doneBtn.setBackgroundColor(Theme.getColor(Theme.key_dialogButton));
        doneBtn.setTextColor(Theme.getColor(Theme.key_chat_botButtonText));
        doneBtn.setActivated(false);
        doneBtn.setOnClickListener(v -> {
            TLRPC.TL_messages_setChatTheme req = new TLRPC.TL_messages_setChatTheme();
            req.emoticon = selectedEmoticon;
            TLRPC.InputPeer peer = null;
            if (chatActivity.getCurrentUser() != null) {
                peer = new TLRPC.TL_inputPeerUser();
                peer.user_id = chatActivity.getCurrentUser().id;
            } else if (chatActivity.getCurrentChat() != null) {
                peer = new TLRPC.TL_inputPeerChat();
                peer.chat_id = chatActivity.getCurrentChat().id;
            }
            if (peer != null) {
                req.peer = peer;
                chatActivity.getConnectionsManager().sendRequest(req,
                        (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                            Log.d("TPV", "DONE BTN REQ");
                        }));
            }

            this.fallbackView();
            chatActivity.clearThemePicker();
        });

        LinearLayout.LayoutParams doneBtnParams = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        doneBtnParams.setMargins(m, m, m, m);
        doneBtn.setLayoutParams(doneBtnParams);


        LinearLayout headerLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams headerParams = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);


        headerParams.setMargins(m, m, m, m);
        headerLayout.setLayoutParams(headerParams);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);

        headerLayout.addView(title);
        headerLayout.addView(darkThemeView);
        headerLayout.addView(dayNight);

        this.addView(headerLayout);


        themeListView = new RecyclerListView(getContext());
        LinearLayout.LayoutParams listParams = LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);

        listParams.setMargins(m, 0, m, 0);
        themeListView.setLayoutParams(listParams);

        this.addView(themeListView);
        this.addView(doneBtn);

        themeListView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));


        themeListView.setAdapter(adapter);

        setupAdapter();
    }

    private void setupAdapter() {
        Utilities.globalQueue.postRunnable(() -> {
            themes = chatActivity.getChatThemesController().getThemes();

            ArrayList<ChatThemesController.ThemePreview> items = new ArrayList<>(themes.size() + 1);
            for (Map.Entry<String, ChatThemesController.ThemePreview> entry : themes.entrySet()) {

                ChatThemesController.ThemePreview theme = entry.getValue();
                if (theme.lightTheme == null || theme.darkTheme == null) {
                    continue;
                }
                items.add(theme);
            }


            if (items.size() == 0) {
                //TODO wait
            } else {
                adapter.setItems(items);
            }
        });
    }

    public void fallbackView() {
        if (selectedEmoticon == null || selectedEmoticon.isEmpty()) {
            if (adapter.getItemCount() > 0 && adapter.items.get(0) instanceof ThemeAdapter.NoThemeCell) {
                Theme.applyTheme(chatActivity.getFallBackTheme());
                fallbackTheme = null;
                return;
            }
        }

        if (fallbackTheme != null) {
            if (selectedEmoticon == null || selectedEmoticon.isEmpty()) {
                Theme.applyTheme(fallbackTheme);
                fallbackTheme = null;
            }
        }
    }

    public void selectTheme(String theme_emoticon) {
        if (!theme_emoticon.equals(selectedEmoticon)) {
            selectedEmoticon = theme_emoticon;
            adapter.notifyDataSetChanged();
        }
    }

    private class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
        boolean isDarkMode;

        ArrayList<ChatThemesController.ThemePreview> items = new ArrayList<>();

        public ThemeAdapter() {
            isDarkMode = Theme.isCurrentThemeDark();
        }

        public void setItems(ArrayList<ChatThemesController.ThemePreview> items) {
            this.items = items;
            addNoThemeCell();
            notifyDataSetChanged();
        }

        private ChatThemesController.ThemePreview getSelectedTheme() {
            if (selectedEmoticon == null || selectedEmoticon.isEmpty()) {
                return null;
            }

            if (themes.containsKey(selectedEmoticon)) {
                return themes.get(selectedEmoticon);
            }
            return null;
        }

        public void switchMode() {
            Log.d("TPV", "switch mode");
            isDarkMode = !isDarkMode;
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);
            String dayThemeName = preferences.getString("lastDayTheme", "Blue");
            if (Theme.getTheme(dayThemeName) == null) {
                dayThemeName = "Blue";
            }
            String nightThemeName = preferences.getString("lastDarkTheme", "Dark Blue");
            if (Theme.getTheme(nightThemeName) == null) {
                nightThemeName = "Dark Blue";
            }

            ChatThemesController.ThemePreview current = getSelectedTheme();
            if (current != null) {
                Theme.ThemeInfo themeInfo = isDarkMode ? current.darkTheme : current.lightTheme;


                if (isDarkMode) {
                    sunDrawable.setCustomEndFrame(0);
                } else {
                    sunDrawable.setCustomEndFrame(36);
                }

                darkThemeView.playAnimation();
                int[] pos = new int[2];
                darkThemeView.getLocationInWindow(pos);
                pos[0] += darkThemeView.getMeasuredWidth() / 2;
                pos[1] += darkThemeView.getMeasuredHeight() / 2;
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme,
                        themeInfo, false, pos, -1, isDarkMode, darkThemeView);
                notifyDataSetChanged();
                invalidate();
                return;


            }

            Theme.ThemeInfo themeInfo = Theme.getActiveTheme();


            if (dayThemeName.equals(nightThemeName)) {
                if (themeInfo.isDark() || dayThemeName.equals("Dark Blue") || dayThemeName.equals("Night")) {
                    dayThemeName = "Blue";
                } else {
                    nightThemeName = "Dark Blue";
                }
            }

            boolean toDark;
            if (toDark = dayThemeName.equals(themeInfo.getKey())) {
                themeInfo = Theme.getTheme(nightThemeName);
                sunDrawable.setCustomEndFrame(36);
            } else {
                themeInfo = Theme.getTheme(dayThemeName);
                sunDrawable.setCustomEndFrame(0);
            }

            darkThemeView.playAnimation();
            if (Theme.selectedAutoNightType != Theme.AUTO_NIGHT_TYPE_NONE) {
                Toast.makeText(getContext(), LocaleController.getString("AutoNightModeOff", R.string.AutoNightModeOff), Toast.LENGTH_SHORT).show();
                Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
                Theme.saveAutoNightThemeConfig();
                Theme.cancelAutoNightThemeCallbacks();
            }

            int[] pos = new int[2];
            darkThemeView.getLocationInWindow(pos);
            pos[0] += darkThemeView.getMeasuredWidth() / 2;
            pos[1] += darkThemeView.getMeasuredHeight() / 2;
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme,
                    themeInfo, false, pos, -1, toDark, darkThemeView);

            notifyDataSetChanged();
            invalidate();
        }


        @NonNull
        @Override
        public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(parent.getContext());
            LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);

            frameParams.setMargins(AndroidUtilities.dp(5), 0, AndroidUtilities.dp(5), 0);

            return new ThemeViewHolder(frame);
        }

        @Override
        public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
            Log.d("TPV", "POS: " + position);
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }


        public class ThemeViewHolder extends RecyclerView.ViewHolder {
            Drawable inDrawable;
            Drawable outDrawable;
            View itemView;

            public ThemeViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
                this.itemView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,LayoutHelper.MATCH_PARENT));

                inDrawable = itemView.getContext().getResources().getDrawable(R.drawable.minibubble_in).mutate();
                outDrawable = itemView.getContext().getResources().getDrawable(R.drawable.minibubble_out).mutate();
            }


            public void bind(ChatThemesController.ThemePreview val) {

                if (val.emoticon.equals(selectedEmoticon)) {
                    itemView.setBackgroundColor(Theme.getColor(Theme.key_dialogButton));
                } else {
                    itemView.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
                }

                LinearLayout layout = new LinearLayout(itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        AndroidUtilities.dp(70), ViewGroup.LayoutParams.MATCH_PARENT);
                int m = AndroidUtilities.dp(5);
                params.setMargins(m, m, m, m);

                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setLayoutParams(params);

                ((FrameLayout) itemView).addView(layout);

                if (val instanceof NoThemeCell) {
                    layout.setBackgroundColor(Theme.getColor(Theme.key_chat_emojiPanelBackground));

                    TextView textView = new TextView(getContext());
                    textView.setText(R.string.no_theme);

                    layout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

                    layout.setOnClickListener(v -> {
                        selectedEmoticon = "";
                        doneBtn.setText(LocaleController.getString("reset_theme", R.string.reset_theme));
                        doneBtn.setActivated(true);
                        //TODO do via events
                        chatActivity.selectedTheme = selectedEmoticon;

                        int[] pos = new int[2];
                        layout.getLocationInWindow(pos);
                        pos[0] += layout.getMeasuredWidth() / 2;
                        pos[1] += layout.getMeasuredHeight() / 2;

                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme,
                                chatActivity.getFallBackTheme(), false, pos, -1, true, darkThemeView);
                        notifyDataSetChanged();
                        adapter.addNoThemeCell();

                    });

                    return;
                }

                Theme.ThemeInfo themeInfo = isDarkMode ? val.darkTheme : val.lightTheme;

                themeInfo.createBackground(val.file, themeInfo.pathToWallpaper);
                int accentId = themeInfo.createNewAccent(themeInfo.info, UserConfig.selectedAccount).id;
                themeInfo.setCurrentAccentId(accentId);

                inDrawable.setColorFilter(new PorterDuffColorFilter(themeInfo.getPreviewInColor(), PorterDuff.Mode.MULTIPLY));
                outDrawable.setColorFilter(new PorterDuffColorFilter(themeInfo.getPreviewOutColor(), PorterDuff.Mode.MULTIPLY));


                ImageView in = new ImageView(itemView.getContext());
                in.setImageDrawable(inDrawable);


                layout.setBackgroundColor(isDarkMode ? Color.DKGRAY
                        : themeInfo.getPreviewBackgroundColor());


                LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(
                        AndroidUtilities.dp(40), LinearLayout.LayoutParams.WRAP_CONTENT);
                inParams.setMargins(AndroidUtilities.dp(4), AndroidUtilities.dp(10), 0, 0);
                inParams.gravity = Gravity.LEFT;
                in.setLayoutParams(inParams);


                ImageView out = new ImageView(layout.getContext());
                out.setImageDrawable(outDrawable);
                LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(
                        AndroidUtilities.dp(40), LinearLayout.LayoutParams.WRAP_CONTENT);
                outParams.setMargins(0, AndroidUtilities.dp(5), AndroidUtilities.dp(4), 0);
                outParams.gravity = Gravity.RIGHT;
                out.setLayoutParams(outParams);

                TextView emoji = new TextView(layout.getContext());
                emoji.setText(val.emoticon);
                LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                emojiParams.setMargins(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
                emojiParams.gravity = Gravity.CENTER;
                emoji.setLayoutParams(emojiParams);

                layout.addView(in);
                layout.addView(out);
                layout.addView(emoji);


                layout.setOnClickListener(v -> {
                    selectedEmoticon = val.emoticon;
                    doneBtn.setText(LocaleController.getString("apply_theme", R.string.apply_theme) + " " + val.emoticon);
                    doneBtn.setActivated(true);
                    //TODO do via events
                    chatActivity.selectedTheme = selectedEmoticon;
                    int[] pos = new int[2];
                    layout.getLocationInWindow(pos);
                    pos[0] += layout.getMeasuredWidth() / 2;
                    pos[1] += layout.getMeasuredHeight() / 2;

                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme,
                            themeInfo, false, pos, -1, true, darkThemeView);

                    //    Theme.applyTheme(themeInfo);
//chatActivity.invalidateMessagesVisiblePart();
                    invalidate();
                    chatActivity.contentView.invalidate();
                    notifyDataSetChanged();
                    adapter.addNoThemeCell();
                    //darkThemeView.stopAnimation();

                });
            }
        }


        private void addNoThemeCell() {
            if (selectedEmoticon != null && !selectedEmoticon.isEmpty()) {
                if (items.size() > 0 && !(items.get(0) instanceof NoThemeCell)) {
                    items.add(0, new NoThemeCell());
                    //   notifyDataSetChanged();
                }

            }

        }

        private class NoThemeCell extends ChatThemesController.ThemePreview {

            public NoThemeCell() {
                super("");
            }
        }
    }
}
