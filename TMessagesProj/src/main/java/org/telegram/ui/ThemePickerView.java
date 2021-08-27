package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatThemesController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ThemePickerView extends BottomSheet implements BottomSheet.BottomSheetDelegateInterface {

    private final RecyclerListView themeListView;
    private final TextView title;
    private final Button doneBtn;
    private final RadialProgressView progressBar;
    private final LinearLayout root;
    private ThemeAdapter adapter;
    private String fallbackEmoticon = "";
    private boolean fallbackModeIsDark;
    private RLottieImageView darkThemeView;
    private RLottieDrawable sunDrawable;
    private String selectedEmoticon;
    private ChatActivity chatActivity;
    boolean interacted;
    private HashMap<String, ChatThemesController.ThemePreview> themes;
    private boolean ignoreFallback;


    public ThemePickerView(@NonNull Context context, ChatActivity ca, String emoticon) {
        super(context, false);
        this.chatActivity = ca;
        fallbackEmoticon = emoticon;

        selectedEmoticon = chatActivity.selectedTheme;
        setApplyBottomPadding(false);
        setCanDismissWithSwipe(true);

        int m = AndroidUtilities.dp(10);
        adapter = new ThemeAdapter();


        title = new TextView(context);
        title.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        title.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.LEFT | Gravity.CENTER));
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
        darkThemeView.setVisibility(View.GONE);
        darkThemeView.setScaleType(ImageView.ScaleType.CENTER);
        darkThemeView.setAnimation(sunDrawable);
        if (Build.VERSION.SDK_INT >= 21) {
            darkThemeView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getChatColor(Theme.key_listSelector), 1, AndroidUtilities.dp(17)));
            Theme.setRippleDrawableForceSoftware((RippleDrawable) darkThemeView.getBackground());
        }
        darkThemeView.setOnClickListener(v -> {
            if (adapter != null) {
                adapter.switchMode();

                darkThemeView.setScaleType(ImageView.ScaleType.CENTER);
                darkThemeView.setAnimation(sunDrawable);
            }
        });


        darkThemeView.setLayoutParams(LayoutHelper.createLinear(
                AndroidUtilities.dp(10), AndroidUtilities.dp(10), Gravity.RIGHT | Gravity.CENTER));
        darkThemeView.playAnimation();

        doneBtn = new Button(getContext());
        doneBtn.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        doneBtn.setText("Done");


        doneBtn.setEnabled(false);
        doneBtn.setOnClickListener(v -> {
            ignoreFallback = true;

            TLRPC.TL_messages_setChatTheme req = new TLRPC.TL_messages_setChatTheme();
            req.emoticon = selectedEmoticon;
            TLRPC.InputPeer peer = null;
            if (chatActivity.getCurrentChat() != null) {
                peer = new TLRPC.TL_inputPeerChat();
                peer.chat_id = chatActivity.getCurrentChat().id;
            } else if (chatActivity.getCurrentUser() != null) {
                peer = new TLRPC.TL_inputPeerUser();
                peer.user_id = chatActivity.getCurrentUser().id;
            }

            if (peer != null) {
                req.peer = peer;
                chatActivity.getConnectionsManager().sendRequest(req,
                        (response, error) -> AndroidUtilities.runOnUIThread(() -> {

                        }));
            }
            dismiss();
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

        root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setCustomView(root);


        themeListView = new RecyclerListView(getContext());
        themeListView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 0, 0, m / 2, 0, m / 2, 0));
        themeListView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        themeListView.setAdapter(adapter);

        progressBar = new RadialProgressView(context);

        root.addView(headerLayout);
        root.addView(themeListView);
        root.addView(progressBar, LayoutHelper.createLinear(AndroidUtilities.dp(30), AndroidUtilities.dp(30), Gravity.CENTER));
        root.addView(doneBtn);

        applyColors();
        darkThemeView.playAnimation();

        setupAdapter();
    }


    private class CellData extends ChatThemesController.ThemePreview {
        public Drawable wallpaperLight;
        public Drawable wallpaperDark;
        public int inBubbleLightColor;
        public int outBubbleLightColor;
        public int inBubbleDarkColor;
        public int outBubbleDarkColor;

        public CellData(String emoji) {
            super(emoji);
        }
    }


    private void setupAdapter() {
        Utilities.globalQueue.postRunnable(() -> {
            themes = chatActivity.getChatThemesController().getThemes();

            for (Map.Entry<String, ChatThemesController.ThemePreview> entry : themes.entrySet()) {

                ChatThemesController.ThemePreview theme = entry.getValue();
                if (theme.lightTheme == null || theme.darkTheme == null) {
                    continue;
                }

                CellData cellData = new CellData(theme.emoticon);
                cellData.darkTheme = theme.darkTheme;
                cellData.lightTheme = theme.lightTheme;


                //TODO load only one set else in background
                Utilities.globalQueue.postRunnable(() -> {
                    Theme.WpData wpDark = Theme.loadWallpaperPreview(theme.darkTheme);
                    cellData.wallpaperDark = wpDark.drawable;
                    cellData.inBubbleDarkColor = wpDark.inBubbleColor;
                    cellData.outBubbleDarkColor = wpDark.outBubbleColor;

                    Theme.WpData wpLight = Theme.loadWallpaperPreview(theme.lightTheme);
                    cellData.wallpaperLight = wpLight.drawable;
                    cellData.inBubbleLightColor = wpLight.inBubbleColor;
                    cellData.outBubbleLightColor = wpLight.outBubbleColor;

                    AndroidUtilities.runOnUIThread(() -> {
                        adapter.items.add(cellData);
                        adapter.notifyItemInserted(adapter.items.size() - 1);


                        if (adapter.items.size() == 1) {
                            root.removeView(progressBar);
                            darkThemeView.setVisibility(View.VISIBLE);
                            adapter.addNoThemeCell();
                        }

                        if (cellData.emoticon.equals(selectedEmoticon)) {
                            adapter.currentSelectedPos = adapter.items.size() - 1;
                        }
                    });
                });
            }
        });
    }

    @Override
    protected boolean canDismissWithTouchOutside() {
        return false;
    }

    @Override
    protected boolean canDismissWithSwipe() {
        return true;
    }

    @Override
    protected boolean onContainerTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }

        if (event.getY() > getContainerView().getTop()) {
            return false;
        }
        chatActivity.contentView.dispatchTouchEvent(event);
        return true;
    }

    public void fallback() {
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.updateThemePreview, fallbackEmoticon, fallbackModeIsDark);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (!ignoreFallback) {
            fallback();
        }

    }


    public void applyColors() {
        sunDrawable.beginApplyLayerColors();
        int color = Theme.getChatColor(Theme.key_dialogTextBlack);
        sunDrawable.setLayerColor("Sunny.**", color);
        sunDrawable.setLayerColor("Path 1.**", color);
        sunDrawable.setLayerColor("Path.**", color);
        sunDrawable.setLayerColor("Path 2.**", color);
        sunDrawable.setLayerColor("Path 3.**", color);
        sunDrawable.setLayerColor("Path 4.**", color);
        sunDrawable.setLayerColor("Path 5.**", color);
        sunDrawable.setLayerColor("Path 6.**", color);
        sunDrawable.setLayerColor("Path 7.**", color);
        sunDrawable.setLayerColor("Path 8.**", color);
        sunDrawable.setLayerColor("Path 9.**", color);
        sunDrawable.setLayerColor("Path 10.**", color);
        sunDrawable.setLayerColor("Path 11.**", color);
        sunDrawable.setLayerColor("Path 12.**", color);
        sunDrawable.setLayerColor("Path 13.**", color);
        sunDrawable.setLayerColor("Path 14.**", color);
        sunDrawable.setLayerColor("Path 15.**", color);
        sunDrawable.setLayerColor("Path 16.**", color);
        sunDrawable.setLayerColor("Path 17.**", color);
        sunDrawable.setLayerColor("Path 18.**", color);
        sunDrawable.setLayerColor("Path 19.**", color);
        sunDrawable.setLayerColor("Path 20.**", color);
        sunDrawable.setLayerColor("Path 21.**", color);
        sunDrawable.commitApplyLayerColors();

        doneBtn.setBackgroundColor(Theme.getChatColor(Theme.key_dialogButton));
        doneBtn.setTextColor(Theme.getChatColor(Theme.key_dialogTextBlack));

        title.setTextColor(Theme.getChatColor(Theme.key_dialogTextBlack));

        try {
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getChatColor(Theme.key_actionBarDefault), PorterDuff.Mode.MULTIPLY));
            container.invalidate();
        }catch (Throwable e){
            e.printStackTrace();
        }

    }


    public void selectTheme(String theme_emoticon) {
        if (theme_emoticon == null || !theme_emoticon.equals(selectedEmoticon)) {
            selectedEmoticon = theme_emoticon;
            invalidateOptionsMenu();

            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onOpenAnimationStart() {
    }

    @Override
    public void onOpenAnimationEnd() {
    }

    @Override
    public boolean canDismiss() {
        return false;
    }

    private class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {
        boolean isDarkMode;
        int currentSelectedPos = -1;

        ArrayList<CellData> items = new ArrayList<>(9);

        public ThemeAdapter() {

            isDarkMode = Theme.isCurrentThemeDark();
            fallbackModeIsDark = isDarkMode;
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
            isDarkMode = !isDarkMode;

            ChatThemesController.ThemePreview current = getSelectedTheme();
            if (current != null) {
                Theme.ThemeInfo themeInfo = isDarkMode ? current.darkTheme : current.lightTheme;



                if (isDarkMode) {
                    sunDrawable.setCustomEndFrame(0);
                } else {
                    sunDrawable.setCustomEndFrame(sunDrawable.getFramesCount());
                }
                darkThemeView.playAnimation();

                NotificationCenter.getGlobalInstance().postNotificationName(
                        NotificationCenter.updateThemePreview, current.emoticon, isDarkMode, true);

              //  applyColors();
                //TODO uncomment to use theme changing animation
//                int[] pos = new int[2];
//                darkThemeView.getLocationInWindow(pos);
//                pos[0] += darkThemeView.getMeasuredWidth() / 2;
//                pos[1] += darkThemeView.getMeasuredHeight() / 2;
//
//
//                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme,
//                        themeInfo, false, pos, -1, isDarkMode, darkThemeView, true);

                notifyDataSetChanged();

            }
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
            holder.setIsRecyclable(false);
            holder.bind(items.get(position), position);
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
                this.itemView.setLayoutParams(LayoutHelper.createLinear(AndroidUtilities.dp(30), AndroidUtilities.dp(40)));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    itemView.setClipToOutline(true);
                }


                inDrawable = itemView.getContext().getResources().getDrawable(R.drawable.minibubble_in).mutate();
                outDrawable = itemView.getContext().getResources().getDrawable(R.drawable.minibubble_out).mutate();
            }


            public void bind(CellData val, int pos) {

                FrameLayout container = new FrameLayout(getContext());
                container.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


                LinearLayout layout = new LinearLayout(itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                int m = AndroidUtilities.dp(5);
                params.setMargins(m, m, m, m);

                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setLayoutParams(params);


                ((FrameLayout) itemView).addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


                if (val instanceof NoThemeCell) {
                    ThemeThumbnailView thumbnailView = new ThemeThumbnailView(getContext(), null);
                    thumbnailView.isSelected = val.emoticon.equals(selectedEmoticon);
                    if (thumbnailView.isSelected) {
                        currentSelectedPos = pos;
                    }
                    thumbnailView.outlineColor = Theme.getChatColor(Theme.key_dialogButton);
                    thumbnailView.fillColor = Theme.getChatColor(Theme.key_graySection);
                    thumbnailView.update();

                    int mm = AndroidUtilities.dp(2);
                    container.addView(thumbnailView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                            Gravity.CENTER, mm, mm, mm, mm));


                    //  layout.setBackgroundColor(Theme.getChatColor(Theme.key_dialogBackground));
                    container.addView(layout);

                    TextView textView = new TextView(getContext());
                    textView.setTextSize(AndroidUtilities.dp(6));
                    textView.setText("    No\nTheme");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    }


                    TextView emoji = new TextView(layout.getContext());
                    emoji.setTextSize(AndroidUtilities.dp(8));
                    emoji.setText("❌");


                    layout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER, 0, AndroidUtilities.dp(5), 0, 0));

                    layout.addView(emoji, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER, 0,
                            AndroidUtilities.dp(2), 0, 0));

                    container.setOnClickListener(v -> {
                        interacted = true;

//                        if (currentSelectedPos == pos) {
//                            return;
//                        }

                        selectedEmoticon = "";
                        doneBtn.setText(LocaleController.getString("reset_theme", R.string.reset_theme));
                        doneBtn.setEnabled(true);

                        NotificationCenter.getGlobalInstance().postNotificationName(
                                NotificationCenter.updateThemePreview, "", isDarkMode);

                        notifyDataSetChanged();
//                        if (currentSelectedPos >= 0) {
//                            notifyItemChanged(currentSelectedPos);
//                        }
//                        notifyItemChanged(pos);
//                        currentSelectedPos = pos;
                        adapter.addNoThemeCell();
                    });
                    return;
                }

                Theme.ThemeInfo themeInfo = isDarkMode ? val.darkTheme : val.lightTheme;
                Drawable wallpaper = isDarkMode ? val.wallpaperDark : val.wallpaperLight;


//                inDrawable.setColorFilter(new PorterDuffColorFilter(themeInfo.getPreviewInColor(), PorterDuff.Mode.MULTIPLY));
//                outDrawable.setColorFilter(new PorterDuffColorFilter(themeInfo.getPreviewOutColor(), PorterDuff.Mode.MULTIPLY));


                ImageView in = new ImageView(itemView.getContext());
                in.setImageDrawable(inDrawable);
                in.setColorFilter(isDarkMode?val.inBubbleDarkColor:val.inBubbleLightColor);
                LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(
                        AndroidUtilities.dp(55), AndroidUtilities.dp(20));
                inParams.setMargins(AndroidUtilities.dp(10), AndroidUtilities.dp(15), 0, 0);
                inParams.gravity = Gravity.LEFT;
                in.setLayoutParams(inParams);


                ImageView out = new ImageView(layout.getContext());
                out.setImageDrawable(outDrawable);
                out.setColorFilter(isDarkMode?val.outBubbleDarkColor:val.outBubbleLightColor);
                LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(
                        AndroidUtilities.dp(55), AndroidUtilities.dp(20));
                outParams.setMargins(0, AndroidUtilities.dp(5), AndroidUtilities.dp(10), 0);
                outParams.gravity = Gravity.RIGHT;
                out.setLayoutParams(outParams);

                TextView emoji = new TextView(layout.getContext());
                emoji.setTextSize(AndroidUtilities.dp(8));
                emoji.setText(val.emoticon);

                if (wallpaper != null) {
                    ThemeThumbnailView thumbnailView = new ThemeThumbnailView(getContext(), wallpaper);
                    thumbnailView.isSelected = val.emoticon.equals(selectedEmoticon);
               //     if (thumbnailView.isSelected) {
//
//                        currentSelectedPos = pos;
                 //   }
                    thumbnailView.outlineColor = Theme.getChatColor(Theme.key_dialogButton);
                    thumbnailView.update();

                    int mm = AndroidUtilities.dp(2);
                    container.addView(thumbnailView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,
                            Gravity.CENTER, mm, mm, mm, mm));

                }

                layout.addView(in);
                layout.addView(out);
                layout.addView(emoji, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                        LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER, 0, AndroidUtilities.dp(2), 0, 0));

                container.addView(layout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                container.setOnClickListener(v -> {

                    interacted = true;

                    if (currentSelectedPos == pos) {
                        return;
                    }
                    selectedEmoticon = val.emoticon;
                    doneBtn.setEnabled(true);
                    doneBtn.setText(LocaleController.getString("ApplyInChatTheme", R.string.ApplyInChatTheme));
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.updateThemePreview, val.emoticon, isDarkMode);

                    notifyDataSetChanged();
//                    if (currentSelectedPos >= 0) {
//                        notifyItemChanged(currentSelectedPos);
//                    }
//                    notifyItemChanged(pos);
//                    currentSelectedPos = pos;

                    adapter.addNoThemeCell();
                });
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        private void addNoThemeCell() {
            if (selectedEmoticon != null && !selectedEmoticon.isEmpty()) {
                if (items.size() > 0 && !(items.get(0) instanceof NoThemeCell)) {
                    items.add(0, new NoThemeCell());
                    notifyDataSetChanged();
                }
            }
        }

        private class ThemeThumbnailView extends View {

            public boolean isSelected;
            public float radius = 30;
            public int outlineColor = Color.BLUE;
            public int outlineSize = 8;
            public int fillColor;
            private final Drawable wallpaper;
            private Paint outlinePaint;
            //  private Paint bgPaint;


            public ThemeThumbnailView(Context context, Drawable wp) {
                super(context);
                this.wallpaper = wp;

                update();
            }

            public void update() {
                outlinePaint = new Paint();
                outlinePaint.setAntiAlias(true);
                outlinePaint.setColor(outlineColor);
                outlinePaint.setStrokeWidth(outlineSize);
                outlinePaint.setStyle(Paint.Style.STROKE);
//
//                bgPaint = new Paint();
//                bgPaint.setColor(fillColor);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int save = canvas.save();


                Path clip = new Path();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    clip.addRoundRect(1, 1, getWidth() - 1, getHeight() - 1, radius, radius, Path.Direction.CW);
                    clip.close();
                    canvas.clipPath(clip);
                }

                if (wallpaper != null) {
                    wallpaper.setBounds(0, 0, getWidth() * 2, getHeight() * 2);
                    wallpaper.draw(canvas);
                } else {
                    canvas.drawColor(fillColor);
                }


                if (isSelected) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        clip.reset();
                        clip.addRoundRect(outlineSize, outlineSize, getWidth() - outlineSize, getHeight() - outlineSize, radius - 5, radius - 5, Path.Direction.CW);
                        clip.close();
                        canvas.drawPath(clip, outlinePaint);
                    } else {
                        canvas.drawRect(0, 0, getWidth(), getHeight(), outlinePaint);
                    }
                }
                canvas.restoreToCount(save);
            }
        }

        private class NoThemeCell extends CellData {
            public NoThemeCell() {
                super("");
            }
        }




    }
}
