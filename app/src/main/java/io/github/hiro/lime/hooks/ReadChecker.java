package io.github.hiro.lime.hooks;


import static io.github.hiro.lime.Main.limeOptions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
public class ReadChecker implements IHook {
    private SQLiteDatabase limeDatabase;
    private SQLiteDatabase db3 = null;
    private SQLiteDatabase db4 = null;
    private boolean shouldHookOnCreate = false;
    private String currentGroupId = null;


    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.ReadChecker.checked) return;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;


                if (appContext == null) {
                    return;
                }
                File dbFile3 = appContext.getDatabasePath("naver_line");
                File dbFile4 = appContext.getDatabasePath("contact");
                if (dbFile3.exists() && dbFile4.exists()) {
                    SQLiteDatabase.OpenParams.Builder builder1 = new SQLiteDatabase.OpenParams.Builder();
                    builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams1 = builder1.build();


                    SQLiteDatabase.OpenParams.Builder builder2 = new SQLiteDatabase.OpenParams.Builder();
                    builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams2 = builder2.build();


                    db3 = SQLiteDatabase.openDatabase(dbFile3, dbParams1);
                    db4 = SQLiteDatabase.openDatabase(dbFile4, dbParams2);


                    Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                            "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                    initializeLimeDatabase(appContext);
                    catchNotification(loadPackageParam, db3, db4, appContext, moduleContext);
                }
            }
        });


        Class<?> chatHistoryRequestClass = XposedHelpers.findClass("com.linecorp.line.chat.request.ChatHistoryRequest", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(chatHistoryRequestClass, "getChatId", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String chatId = (String) param.getResult();
                //XposedBridge.log(chatId);
                if (isGroupExists(chatId)) {
                    shouldHookOnCreate = true;
                    currentGroupId = chatId;
                } else {
                    shouldHookOnCreate = false;
                    currentGroupId = null;
                }
            }
        });


        Class<?> chatHistoryActivityClass = XposedHelpers.findClass("jp.naver.line.android.activity.chathistory.ChatHistoryActivity", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(chatHistoryActivityClass, "onCreate", Bundle.class, new XC_MethodHook() {
            Context moduleContext;


            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (moduleContext == null) {
                    try {
                        Context systemContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                        moduleContext = systemContext.createPackageContext("io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                    } catch (Exception e) {
                        //XposedBridge.log("Failed to get module context: " + e.getMessage());
                    }
                }
            }


            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (moduleContext == null) {
                    //XposedBridge.log("Module context is null. Skipping hook.");
                    return;
                }


                if (shouldHookOnCreate && currentGroupId != null) {
                    if (!isNoGroup(currentGroupId)) {
                        Activity activity = (Activity) param.thisObject;


                        addButton(activity, moduleContext);
                    }
                }
            }
        });


    }


    private boolean isGroupExists(String groupId) {
        if (limeDatabase == null) {
            //XposedBridge.log("Database is not initialized.");
            return false;
        }
        String query = "SELECT 1 FROM read_message WHERE group_id = ?";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }


    private boolean isNoGroup(String groupId) {
        if (limeDatabase == null) {
            //XposedBridge.log("Database is not initialized.");
            return true;
        }
        String query = "SELECT group_name FROM read_message WHERE group_id = ?";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId});
        boolean noGroup = true;
        if (cursor.moveToFirst()) {
            String groupName = cursor.getString(cursor.getColumnIndexOrThrow("group_name"));
            noGroup = groupName == null || groupName.isEmpty();
        }


        cursor.close();
        return noGroup;
    }


    private void addButton(Activity activity, Context moduleContext) {
        // ファイルパスを取得
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        File file = new File(dir, "margin_settings.txt");

        // デフォルト値
        float readCheckerHorizontalMarginFactor = 0.5f; // デフォルト値
        int readCheckerVerticalMarginDp = 100; // デフォルト値
        float readCheckerSizeDp = 60; // デフォルト値

        // ファイルの内容を読み込む
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        if (parts[0].trim().equals("Read_checker_horizontalMarginFactor")) {
                            readCheckerHorizontalMarginFactor = Float.parseFloat(parts[1].trim());
                        } else if (parts[0].trim().equals("Read_checker_verticalMarginDp")) {
                            readCheckerVerticalMarginDp = Integer.parseInt(parts[1].trim());
                        } else if (parts[0].trim().equals("chat_read_check_size")) {
                            readCheckerSizeDp = Float.parseFloat(parts[1].trim());
                        }
                    }
                }
            } catch (IOException | NumberFormatException ignored) {
            }
        }

        ImageView imageButton = new ImageView(activity);
        String imageName = "read_checker.png";
        File imageFile = new File(dir, imageName);

        if (!imageFile.exists()) {
            try (InputStream in = moduleContext.getResources().openRawResource(
                    moduleContext.getResources().getIdentifier(imageName.replace(".png", ""), "drawable", "io.github.hiro.lime"));
                 OutputStream out = new FileOutputStream(imageFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (imageFile.exists()) {
            Drawable drawable = Drawable.createFromPath(imageFile.getAbsolutePath());
            if (drawable != null) {
                int sizeInPx = dpToPx(moduleContext, readCheckerSizeDp);
                drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                imageButton.setImageDrawable(drawable);
            }
        }

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        int horizontalMarginPx = (int) (readCheckerHorizontalMarginFactor * activity.getResources().getDisplayMetrics().widthPixels);
        int verticalMarginPx = (int) (readCheckerVerticalMarginDp * activity.getResources().getDisplayMetrics().density);
        frameParams.setMargins(horizontalMarginPx, verticalMarginPx, 0, 0);

        imageButton.setLayoutParams(frameParams);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGroupId != null) {
                    showDataForGroupId(activity, currentGroupId, moduleContext);
                }
            }
        });

        ViewGroup layout = activity.findViewById(android.R.id.content);
        layout.addView(imageButton);
    }

    private int dpToPx(@NonNull Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private Drawable scaleDrawable(Drawable drawable, int width, int height) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return new BitmapDrawable(scaledBitmap);
    }


    private void showDataForGroupId(Activity activity, String groupId, Context moduleContext) {
        if (limeDatabase == null) {
            return;
        }

        // SQLクエリの初期化
        String query;
        if (limeOptions.MySendMessage.checked) {
            // Send_User が null のメッセージのみを取得するクエリ
            query = "SELECT server_id, content, created_time FROM read_message WHERE group_id=? AND Send_User IS NULL ORDER BY created_time ASC";
        } else {
            // 通常のクエリ
            query = "SELECT server_id, content, created_time FROM read_message WHERE group_id=? ORDER BY created_time ASC";
        }

        Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId});

        Map<String, DataItem> dataItemMap = new HashMap<>();

        while (cursor.moveToNext()) {
            String serverId = cursor.getString(0);
            String content = cursor.getString(1);
            String createdTime = cursor.getString(2);

            List<String> user_nameList = getuser_namesForServerId(serverId);

            if (dataItemMap.containsKey(serverId)) {
                DataItem existingItem = dataItemMap.get(serverId);
                existingItem.user_names.addAll(user_nameList);
            } else {
                DataItem dataItem = new DataItem(serverId, content, createdTime);
                dataItem.user_names.addAll(user_nameList);
                dataItemMap.put(serverId, dataItem);
            }
        }
        cursor.close();

        List<DataItem> sortedDataItems = new ArrayList<>(dataItemMap.values());
        Collections.sort(sortedDataItems, Comparator.comparing(item -> item.createdTime));

        StringBuilder resultBuilder = new StringBuilder();
        for (DataItem item : sortedDataItems) {
            resultBuilder.append("Content: ").append(item.content != null ? item.content : "Media").append("\n");
            resultBuilder.append("Created Time: ").append(item.createdTime).append("\n");

            if (!item.user_names.isEmpty()) {
                int newlineCount = 0;
                for (String user_name : item.user_names) {
                    newlineCount += countNewlines(user_name);
                }
                resultBuilder.append(moduleContext.getResources().getString(R.string.Reader))
                        .append(" (").append(item.user_names.size() + newlineCount).append("):\n");
                for (String user_name : item.user_names) {
                    resultBuilder.append("").append(user_name).append("\n");
                }
            } else {
                resultBuilder.append("No talk names found.\n");
            }
            resultBuilder.append("\n");
        }

        TextView textView = new TextView(activity);
        textView.setText(resultBuilder.toString());
        textView.setPadding(20, 20, 20, 20);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(textView);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("READ Data");
        builder.setView(scrollView);

        builder.setPositiveButton("OK", null);

        builder.setNegativeButton(moduleContext.getResources().getString(R.string.Delete), (dialog, which) -> {
            new AlertDialog.Builder(activity)
                    .setTitle(moduleContext.getResources().getString(R.string.check))
                    .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                    .setPositiveButton(moduleContext.getResources().getString(R.string.yes), (confirmDialog, confirmWhich) -> deleteGroupData(groupId, activity, moduleContext))
                    .setNegativeButton(moduleContext.getResources().getString(R.string.no), null)
                    .show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    private void deleteGroupData(String groupId, Activity activity, Context moduleContext) {
        if (limeDatabase == null) {
            return;
        }


        String deleteQuery = "DELETE FROM read_message WHERE group_id=?";
        limeDatabase.execSQL(deleteQuery, new String[]{groupId});
        Toast.makeText(activity, moduleContext.getResources().getString(R.string.Reader_Data_Delete_Success), Toast.LENGTH_SHORT).show();
    }


    private List<String> getuser_namesForServerId(String serverId) {
        if (limeDatabase == null) {
            return Collections.emptyList();
        }
        String query = "SELECT user_name FROM read_message WHERE server_id=? ORDER BY created_time ASC";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{serverId});
       List<String> userNames = new ArrayList<>();

        while (cursor.moveToNext()) {
            String userNameStr = cursor.getString(0);
            if (userNameStr != null) {
                // user_nameをそのままリストに追加
                userNames.add(userNameStr);
            }
        }
        cursor.close();
        return userNames;
    }


    private int countNewlines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }


    private static class DataItem {
        String serverId;
        String content;
        String createdTime;
        Set<String> user_names;


        DataItem(String serverId, String content, String createdTime) {
            this.serverId = serverId;
            this.content = content;
            this.createdTime = createdTime;
            this.user_names = new HashSet<>();
        }
    }

    private void catchNotification(XC_LoadPackage.LoadPackageParam loadPackageParam, SQLiteDatabase db3, SQLiteDatabase db4, Context appContext, Context moduleContext) {
        try {
            XposedBridge.hookAllMethods(
                    loadPackageParam.classLoader.loadClass(Constants.NOTIFICATION_READ_HOOK.className),
                    "invokeSuspend",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String paramValue = param.args[0].toString();
                            if (appContext == null) {

                                return;
                            }


                            Context moduleContext;
                            try {
                                moduleContext = appContext.createPackageContext(
                                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                            } catch (PackageManager.NameNotFoundException e) {
                                XposedBridge.log("Failed to create package context: " + e.getMessage());
                                return;
                            }


                            if (paramValue != null && paramValue.contains("type:NOTIFIED_READ_MESSAGE")) {
                                List<String> messages = extractMessages(paramValue);
                                for (String message : messages) {
                                    fetchDataAndSave(db3, db4, message, appContext, moduleContext);
                                }
                            }
                        }
                    }
            );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private List<String> extractMessages(String paramValue) {
        List<String> messages = new ArrayList<>();
        Pattern pattern = Pattern.compile("type:NOTIFIED_READ_MESSAGE.*?(?=type:|$)");
        Matcher matcher = pattern.matcher(paramValue);


        while (matcher.find()) {
            messages.add(matcher.group().trim());
        }


        return messages;
    }


    private void fetchDataAndSave(SQLiteDatabase db3, SQLiteDatabase db4, String paramValue, Context context, Context moduleContext) {



        try {
            String serverId = extractServerId(paramValue, context);
            String SentUser = extractSentUser(paramValue);
            if (serverId == null || SentUser == null) {
                return;
            }
            String SendUser = queryDatabase(db3, "SELECT from_mid FROM chat_history WHERE server_id=?", serverId);
            String groupId = queryDatabase(db3, "SELECT chat_id FROM chat_history WHERE server_id=?", serverId);
            String groupName = queryDatabase(db3, "SELECT name FROM groups WHERE id=?", groupId);
            String content = queryDatabase(db3, "SELECT content FROM chat_history WHERE server_id=?", serverId);
            String user_name = queryDatabase(db4, "SELECT profile_name FROM contacts WHERE mid=?", SentUser);
            String timeEpochStr = queryDatabase(db3, "SELECT created_time FROM chat_history WHERE server_id=?", serverId);
            String timeFormatted = formatMessageTime(timeEpochStr);
            String media = queryDatabase(db3, "SELECT attachement_type FROM chat_history WHERE server_id=?", serverId);
            String mediaDescription = "";
            if (media != null) {
                switch (media) {
                    case "7":
                        mediaDescription = moduleContext.getResources().getString(R.string.sticker);
                        break;
                    case "1":
                        mediaDescription = moduleContext.getResources().getString(R.string.picture);
                        break;
                    case "2":
                        mediaDescription = moduleContext.getResources().getString(R.string.video);
                        break;
                    default:
                        mediaDescription = "";
                        break;
                }
            }
            String finalContent = (content != null && !content.isEmpty()) ? content : (!mediaDescription.isEmpty() ? mediaDescription : "No content:" + serverId);
            saveData(SendUser, groupId, serverId, SentUser, groupName, finalContent, user_name, timeFormatted, context);
        } catch (Exception e) {
        }
    }



    private void writeToFile(File file, String text) {
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(text + "\n");
        } catch (IOException ignored) {
        }
    }


    private String formatMessageTime(String timeEpochStr) {
        if (timeEpochStr == null) return null;
        long timeEpoch = Long.parseLong(timeEpochStr);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timeEpoch));
    }


    private String extractSentUser(String paramValue) {
        Pattern pattern = Pattern.compile("param2:([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(paramValue);


        return matcher.find() ? matcher.group(1) : null;


    }


    private String extractServerId(String paramValue, Context context) {
        Pattern pattern = Pattern.compile("param3:([0-9]+)");
        Matcher matcher = pattern.matcher(paramValue);
        XposedBridge.log(paramValue);
        if (matcher.find()) {
            return matcher.group(1);


        } else {;
            return null;
        }
    }



    private String queryDatabase(SQLiteDatabase db, String query, String... selectionArgs) {
        if (db == null) {
           XposedBridge.log("Database is not initialized.");
            return null;
        }
        Cursor cursor = db.rawQuery(query, selectionArgs);
        String result = null;
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }


    private void initializeLimeDatabase(Context context) {


        File oldDbFile = new File(context.getFilesDir(), "limes_checked_data.db");
        if (oldDbFile.exists()) {
            boolean deleted = oldDbFile.delete();
            if (deleted) {
                //XposedBridge.log("Old database file lime_data.db deleted.");
            } else {
                //XposedBridge.log("Failed to delete old database file lime_data.db.");
            }
        }
        File dbFile = new File(context.getFilesDir(), "checked_data.db");
        limeDatabase = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        String createGroupTable = "CREATE TABLE IF NOT EXISTS read_message (" +
                "group_id TEXT NOT NULL, " +
                "server_id TEXT NOT NULL, " +
                "Sent_User TEXT, " +
                "Send_User TEXT, " +
                "group_name TEXT, " +
                "content TEXT, " +
                "user_name TEXT, " +
                "created_time TEXT, " +
                "PRIMARY KEY(group_id, server_id, Sent_User, Send_User)" +
                ");";
        limeDatabase.execSQL(createGroupTable);
        XposedBridge.log("Database initialized and read_message table created.");
    }


    private void saveData( String SendUser, String groupId, String serverId, String SentUser, String groupName, String content, String user_name, String createdTime, Context context) {
        if (groupName == null) {


            return;
        }
        Cursor cursor = null;
        try {
            String checkQuery = "SELECT COUNT(*), user_name FROM read_message WHERE server_id=? AND Sent_User=?";
            cursor = limeDatabase.rawQuery(checkQuery, new String[]{serverId});
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                String existingUserName = cursor.getString(1);
                String currentTime = getCurrentTime();


                if (count > 0) {
                    if (!existingUserName.contains(user_name)) {
                        String updatedUserName = existingUserName + (existingUserName.isEmpty() ? "" : "\n") + "-" + user_name + " [" + currentTime + "]";
                        ContentValues values = new ContentValues();
                        values.put("user_name", updatedUserName);
                        limeDatabase.update("read_message", values, "server_id=? AND Sent_User=?", new String[]{serverId});
                        XposedBridge.log("User name updated for server_id: " + serverId + ", Sent_User: " + SentUser);
                    }
                } else {
                    insertNewRecord(SendUser, groupId, serverId, SentUser, groupName, content, "-" + user_name + " [" + currentTime + "]", createdTime);
                }
                updateOtherRecordsUserNames(groupId, user_name, currentTime);
            }


        } catch (Exception e) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    private void updateOtherRecordsUserNames(String groupId, String user_name, String currentTime) {
        Cursor cursor = null;
        try {
            String selectOtherQuery = "SELECT server_id, user_name FROM read_message WHERE group_id=? AND user_name NOT LIKE ?";
            cursor = limeDatabase.rawQuery(selectOtherQuery, new String[]{groupId, "%-" + user_name + "%"});


            while (cursor.moveToNext()) {
                String serverId = cursor.getString(cursor.getColumnIndexOrThrow("server_id"));
                String existingUserName = cursor.getString(cursor.getColumnIndexOrThrow("user_name"));


                if (!existingUserName.contains(user_name)) {
                    String updatedUserName = existingUserName + (existingUserName.isEmpty() ? "" : "\n") + "-" + user_name + " [" + currentTime + "]";
                    ContentValues values = new ContentValues();
                    values.put("user_name", updatedUserName);
                    limeDatabase.update("read_message", values, "group_id=? AND server_id=?", new String[]{groupId, serverId});
                    //XposedBridge.log("Updated user_name for other records in group_id: " + groupId + ", server_id: " + serverId);
                }
            }
        } catch (Exception e) {
            Log.e("updateOtherRecordsUserNames", "Error updating other records' user names:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }


    private void insertNewRecord(String SendUser, String groupId, String serverId, String SentUser, String groupName, String content, String user_name, String createdTime) {
    XposedBridge.log("Attempting to insert new record: server_id=" + serverId + ", Sent_User=" + SentUser);
    XposedBridge.log("Inserting values: groupId=" + groupId + ", serverId=" + serverId + ", SentUser=" + SentUser + ", SendUser=" + SendUser + ", groupName=" + groupName + ", content=" + content + ", user_name=" + user_name + ", createdTime=" + createdTime);


    String insertQuery = "INSERT INTO read_message(group_id, server_id, Sent_User, Send_User, group_name, content, user_name, created_time)" +
            " VALUES(?, ?, ?, ?, ?, ?, ?, ?);";

    try {
        // トランザクションを開始
        limeDatabase.beginTransaction();

        // SQLクエリを実行
        limeDatabase.execSQL(insertQuery, new Object[]{groupId, serverId, SentUser, SendUser, groupName, content, user_name, createdTime});

        // トランザクションを成功としてマーク
      limeDatabase.setTransactionSuccessful();

        // ログ出力
        XposedBridge.log("New record inserted successfully: server_id=" + serverId + ", Sent_User=" + SentUser);
    } catch (Exception e) {
        // エラーログ出力
        XposedBridge.log("Error inserting new record: " + e.getMessage());
        e.printStackTrace();
    } finally {
        limeDatabase.endTransaction();
    }
}

}
