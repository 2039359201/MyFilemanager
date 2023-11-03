package com.example.myfilemanager;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    TextView pathTv;
    ImageButton backBtn;
    ListView fileLv;
    File currentParent;
    File[] currentFiles;
    File root;
    private ActionMode actionMode;
    private AdapterView.OnItemClickListener fileItemClickListener;

    private boolean isCutOperation = false;
    // 定义请求权限的标识
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 2;

    private ArrayList<String> copiedFilePaths = new ArrayList<>();

    private ArrayList<File> selectedFiles = new ArrayList<>();

    //  filteredFiles 列表
    private List<File> filteredFiles = new ArrayList<>();

    // 在 MainActivity 中添加显示文件后缀的状态成员变量
    private boolean showFileExtensions = true;


    //排序方式
    private int sortOrder = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pathTv = findViewById(R.id.id_tv_filepath);
        backBtn = findViewById(R.id.id_btn_back);
        backBtn.setVisibility(View.INVISIBLE);  // 设置为不可见
        backBtn.setEnabled(false);  // 设置为不可点击
        fileLv = findViewById(R.id.id_lv_file);
        // 获取 TextView用于搜索监听器
        TextView pathTv = findViewById(R.id.id_tv_filepath);
        // 为 TextView 设置点击监听器
        pathTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 在这里执行搜索文件的操作，可以启动一个新的 Activity 或者弹出搜索框等
                showSearchDialog();
            }
        });


        boolean isLoadSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (isLoadSDCard) {
//            获取sd卡的根目录
            root = Environment.getExternalStorageDirectory();
            currentParent = root;
//            获取当前文件夹下的所有文件
            currentFiles = currentParent.listFiles();
            //Log.i("animee", "onCreate:currentFiles: "+currentFiles.length);
            inflateListView(currentFiles);
        }else {
            Toast.makeText(this,"SD卡没有被装载",Toast.LENGTH_SHORT).show();
        }


        // 检查是否有写入外部存储的权限，如果没有，则请求权限
        // 检查是否有 MANAGE_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // 请求权限
                requestManageExternalStoragePermission();
            }
        }

        //创建文件按钮
        ImageButton createFileBtn;
        createFileBtn = findViewById(R.id.createFileBtn);
        createFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateFileDialog();
            }
        });

        // 设置长按监听器以启动上下文操作模式
        fileLv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        fileLv.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // 根据项目选择更新selectedFiles列表
                if (checked) {
                    selectedFiles.add(currentFiles[position]);
                } else {
                    selectedFiles.remove(currentFiles[position]);
                }

                // 更新上下文操作模式的标题
                mode.setTitle(selectedFiles.size() + " 个文件被选中");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // 为上下文操作模式膨胀菜单
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.contextual_menu, menu);
                return true;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // 根据选定的菜单项处理操作（复制、剪切、删除）
                if (item.getItemId() == R.id.menu_copy) {
                    // 实现复制操作
                    copySelectedFiles();
                } else if (item.getItemId() == R.id.menu_cut) {
                    // 实现剪切操作
                    cutSelectedFiles();
                } else if (item.getItemId() == R.id.menu_paste) {
                    // 实现粘贴操作
                    pasteSelectedFiles();
                } else if (item.getItemId() == R.id.menu_delete) {
                    // 实现删除操作
                    deleteSelectedFiles();
                } else if (item.getItemId()== R.id.menu_dot){
                    //实现后缀显示隐藏切换操作
                    showFileExtensions = !showFileExtensions;
                    inflateListView(currentFiles);
                }else if (item.getItemId()== R.id.menu_namesort){

                    sortOrder =1;
                    inflateListView(currentFiles);
                }else if (item.getItemId()== R.id.menu_timesort){

                    sortOrder =2;
                    inflateListView(currentFiles);
                }else if (item.getItemId()== R.id.menu_sizesort){

                    sortOrder =3;
                    inflateListView(currentFiles);
                }
                mode.finish(); // 在处理操作后结束上下文操作模式
                return true;
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // 在上下文操作模式销毁时清除selectedFiles列表
                selectedFiles.clear();
                actionMode = null;
            }
        });


            // 为ListView设置长按监听器以处理删除操作
//        fileLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                showDeleteFileDialog(position);
//                return true;
//            }
//        });
        fileLv.setOnItemLongClickListener(null);

        // 设置文件项点击监听器以处理打开目录
        fileItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File clickedFile = currentFiles[position];

                if (clickedFile.isFile()) {
                    openFile(clickedFile);
                } else {
                    // 如果是文件夹，进入文件夹
                    currentParent = clickedFile;
                    currentFiles = currentParent.listFiles();
                    inflateListView(currentFiles);
                }
            }
        };
        fileLv.setOnItemClickListener(fileItemClickListener);


        //        设置监听事件
        setListener();


    }

    // 添加 showSearchDialog 方法
    private void showSearchDialog() {
        // 创建一个对话框，包含一个编辑框供用户输入搜索内容
        final EditText searchEditText = new EditText(MainActivity.this);
        searchEditText.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("搜索文件");
        builder.setMessage("输入文件名或路径");
        builder.setView(searchEditText);
        builder.setPositiveButton("搜索", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String searchText = searchEditText.getText().toString().trim();
                // 在这里执行搜索操作，可以根据输入的文本进行文件搜索
                // 可以使用 Intent 启动包含搜索结果的新 Activity，或者更新当前页面的文件列表
                searchFiles(searchText, currentFiles);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    // 添加 searchFiles 方法
    private void searchFiles(String searchText, File[] files) {
        List<File> searchResults = new ArrayList<>();
        // 遍历当前文件列表，找到包含搜索文本的文件
        for (File file : files) {
            if (file.getName().toLowerCase().contains(searchText.toLowerCase())) {
                searchResults.add(file);
            }
            if (file.isDirectory()) {
                // 如果是文件夹，递归搜索其内容
                File[] subFiles = file.listFiles();
                if (subFiles != null && subFiles.length > 0) {
                    searchFiles(searchText, subFiles);
                }
            }
        }

        if (searchResults.isEmpty() && files == currentFiles) {
            // 只在最外层调用时显示未找到匹配文件的提示
            Toast.makeText(MainActivity.this, "未找到匹配的文件", Toast.LENGTH_SHORT).show();
        } else if (!searchResults.isEmpty()) {
            // 显示搜索结果
            currentFiles = searchResults.toArray(new File[0]);
            inflateListView(currentFiles);
        }
    }

    private void copySelectedFiles() {
        if (selectedFiles.isEmpty()) {
            Toast.makeText(MainActivity.this, "没有文件可以复制", Toast.LENGTH_SHORT).show();
            return;
        }
        // 清空已经复制的文件列表
        copiedFilePaths.clear();

        // 设置剪贴板内容为文件路径和操作类型
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            // 将选定文件的路径列表转换为字符串，并使用逗号分隔
            StringBuilder builder = new StringBuilder();
            for (File file : selectedFiles) {
                copiedFilePaths.add(file.getAbsolutePath());
                builder.append(file.getAbsolutePath()).append(",");
            }

            builder.deleteCharAt(builder.length() - 1); // 删除末尾的逗号

            ClipData clipData = ClipData.newPlainText("file_path", selectedFiles.get(0).getAbsolutePath());
            clipboard.setPrimaryClip(clipData);
            isCutOperation = false;
        }
    }

    private void cutSelectedFiles() {
        // 清空已经剪切的文件列表
        copiedFilePaths.clear();

        // 设置剪贴板内容为文件路径列表和操作类型
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            // 将选定文件的路径列表转换为字符串，并使用逗号分隔
            StringBuilder builder = new StringBuilder();
            for (File file : selectedFiles) {
                copiedFilePaths.add(file.getAbsolutePath());
                builder.append(file.getAbsolutePath()).append(",");
            }
            builder.deleteCharAt(builder.length() - 1); // 删除末尾的逗号

            ClipData clipData = ClipData.newPlainText("file_paths", builder.toString());
            clipboard.setPrimaryClip(clipData);
            isCutOperation = true;
        }
    }

    private void pasteSelectedFiles() {
        if (copiedFilePaths.isEmpty()) {
            Toast.makeText(MainActivity.this, "剪贴板中没有文件", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String copiedFilePath : copiedFilePaths) {
            if (isCutOperation) {
                pasteCutFile(copiedFilePath);
            } else {
                pasteCopyFile(copiedFilePath);
            }
        }

        // 清空剪贴板内容和操作状态
        copiedFilePaths.clear();
        isCutOperation = false;

        // 更新文件列表
        currentFiles = currentParent.listFiles();
        inflateListView(currentFiles);

        Toast.makeText(MainActivity.this, "文件已全部粘贴", Toast.LENGTH_SHORT).show();
    }
    private void pasteCopyFile(String copiedFilePath) {
        File sourceFile = new File(copiedFilePath);
        File destinationFile = new File(currentParent, sourceFile.getName());

        try {
            // 使用 FileInputStream 和 FileOutputStream 进行文件复制
            FileInputStream inputStream = new FileInputStream(sourceFile);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            inputStream.close();
            outputStream.close();

            // 更新文件列表
            currentFiles = currentParent.listFiles();
            inflateListView(currentFiles);

            Toast.makeText(MainActivity.this, "文件已粘贴", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "无法粘贴文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteCutFile(String copiedFilePath) {
        File sourceFile = new File(copiedFilePath);
        File destinationFile = new File(currentParent, sourceFile.getName());

        try {
            // 使用 FileInputStream 和 FileOutputStream 进行文件复制
            FileInputStream inputStream = new FileInputStream(sourceFile);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            inputStream.close();
            outputStream.close();

            // 删除原文件
            sourceFile.delete();

            // 更新文件列表
            currentFiles = currentParent.listFiles();
            inflateListView(currentFiles);

            Toast.makeText(MainActivity.this, "文件已剪切", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "无法剪切文件", Toast.LENGTH_SHORT).show();
        }
    }
    private void showCreateFileDialog() {
        // 创建一个对话框，用于输入文件名
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("输入文件(夹)名");

        // 弹出对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("创建文件(夹)");
        builder.setView(input);
        builder.setPositiveButton("创建", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString().trim();

                if (!fileName.isEmpty()) {
                    // 判断文件名是否包含后缀
                    if (fileName.contains(".")) {
                        // 创建文本文件
                        createFile(fileName);
                    } else {
                        // 创建文件夹
                        createFolder(fileName);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "名称不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void createFile(String fileName) {
        // 创建文件对象
        File newFile = new File(currentParent, fileName);

        try {
            // 创建文件
            if (newFile.createNewFile()) {
                // 文件创建成功
                Toast.makeText(MainActivity.this, "文件创建成功", Toast.LENGTH_SHORT).show();
                // 重新加载文件列表
                currentFiles = currentParent.listFiles();
                inflateListView(currentFiles);
            } else {
                // 文件已存在
                Toast.makeText(MainActivity.this, "文件已存在", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "无法创建文件", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加 createFolder 方法
    private void createFolder(String folderName) {
        // 创建文件夹对象
        File newFolder = new File(currentParent, folderName);

        if (newFolder.mkdir()) {
            // 文件夹创建成功
            Toast.makeText(MainActivity.this, "文件夹创建成功", Toast.LENGTH_SHORT).show();
            // 重新加载文件列表
            currentFiles = currentParent.listFiles();
            inflateListView(currentFiles);
            // 添加设置文件夹权限的代码
            setFolderPermissions(newFolder);
        } else {
            // 文件夹已存在或创建失败
            Toast.makeText(MainActivity.this, "文件夹已存在或创建失败", Toast.LENGTH_SHORT).show();
        }
    }
    // 添加 setFolderPermissions 方法
    private void setFolderPermissions(File folder) {
        try {
            // 设置文件夹权限为可读可写（可根据需求进行修改）
            Runtime.getRuntime().exec("chmod 777 " + folder.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "无法设置文件夹权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestManageExternalStoragePermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            // 用户返回应用后，检查权限状态
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 用户授予了 MANAGE_EXTERNAL_STORAGE 权限
                    // 在此处可以进行相关的文件访问操作
                } else {
                    // 用户未授予 MANAGE_EXTERNAL_STORAGE 权限
                    Toast.makeText(this, "未授予文件管理权限，某些功能可能受限", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private void deleteSelectedFiles() {
        for (File file : selectedFiles) {
            if (file.isDirectory()) {
                // 如果是目录，首先递归删除其内容
                deleteDirectory(file);
            }

            // 删除文件
            boolean success = file.delete();

            if (!success) {
                Toast.makeText(MainActivity.this, "无法删除文件：" + file.getName(), Toast.LENGTH_SHORT).show();
            }
        }

        // 重新加载文件列表
        currentFiles = currentParent.listFiles();
        inflateListView(currentFiles);

        // 清空选中的文件列表
        selectedFiles.clear();
    }


    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 处理权限请求的结果
        if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，可以执行相关操作
            } else {
                // 权限被拒绝，显示提示信息或采取其他适当的操作
                Toast.makeText(this, "权限被拒绝，无法进行操作", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void setListener() {
        fileLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File clickedFile = currentFiles[position];

                if (clickedFile.isFile()) {
                    openFile(clickedFile);
                } else {
                    // 如果是文件夹，进入文件夹
                    currentParent = clickedFile;
                    currentFiles = currentParent.listFiles();
                    inflateListView(currentFiles);
                }

            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * 判断当前的目录是否为sd卡的根目录，如果是根目录，就直接退出activity。
                 * 如果不是根目录，就获取当前目录的父目录，然后获得父目录的文件，在重新加载listview
                 * */
                if (currentParent.getAbsolutePath().equals(root.getAbsolutePath())) {
                    MainActivity.this.finish();
                } else {
                    currentParent = currentParent.getParentFile();
                    currentFiles = currentParent.listFiles();
                    inflateListView(currentFiles);
                }
            }
        });
    }



    // 添加 openFile 方法
    private void openFile(File file) {
        // 获取 MIME 类型
        String mimeType = getMimeType(file);

        if (mimeType != null) {
            // 通过 FileProvider 获取内容 URI
            Uri contentUri = FileProvider.getUriForFile(this, "com.example.myfilemanager.fileprovider", file);

            // 创建打开文件的 Intent
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(contentUri, mimeType);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // 授予 URI 读取权限

            try {
                startActivity(openIntent);
            } catch (ActivityNotFoundException e) {
                // 如果没有适合的应用程序处理该 MIME 类型，显示提示信息
                Toast.makeText(MainActivity.this, "没有适合的应用程序处理此文件类型", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 未能获取文件的 MIME 类型
            Toast.makeText(MainActivity.this, "无法确定文件类型", Toast.LENGTH_SHORT).show();
        }
    }
    // 添加 getMimeType 方法
    private String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }





//    private void inflateListView(File[] currentFiles) {
//        List<Map<String,Object>> list = new ArrayList<>();
//        for (int i = 0; i < currentFiles.length; i++) {
//            Map<String,Object> map = new HashMap<>();
//            map.put("filename", currentFiles[i].getName());
//            if (currentFiles[i].isFile()) {
//                map.put("icon", R.mipmap.file);
//            } else {
//                map.put("icon", R.mipmap.folder);
//            }
//            list.add(map);
//        }
//
//        // 创建适配器对象
//        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.item_file_explorer,
//                new String[]{"filename", "icon"}, new int[]{R.id.item_tv, R.id.item_icon});
//
//        // 设置适配器
//        fileLv.setAdapter(adapter);
//    }
//    private void inflateListView(File[] currentFiles) {
//        List<Map<String,Object>> list = new ArrayList<>();
//        for (int i = 0; i < currentFiles.length; i++) {
//            Map<String,Object>map = new HashMap<>();
//            map.put("filename",currentFiles[i].getName());
//            if (currentFiles[i].isFile()) {
//                map.put("icon",R.mipmap.file);
//            }else {
//                map.put("icon",R.mipmap.folder);
//            }
//            list.add(map);
//        }
////       创建适配器对象
//        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.item_file_explorer, new String[]{"filename", "icon"}, new int[]{R.id.item_tv, R.id.item_icon});
//        fileLv.setAdapter(adapter);
//        pathTv.setText("当前路径:"+currentParent.getAbsolutePath());
//    }
    private void inflateListView(File[] currentFiles) {
        List<Map<String, Object>> list = new ArrayList<>();
        Arrays.sort(currentFiles, new FileComparator(sortOrder));

        for (int i = 0; i < currentFiles.length; i++) {
            Map<String, Object> map = new HashMap<>();
            String fileName = showFileExtensions ? currentFiles[i].getName() : removeFileExtension(currentFiles[i].getName());
            map.put("filename", fileName);
            if (currentFiles[i].isFile()) {
                map.put("icon", R.mipmap.file_kawayi);
            } else {

                map.put("icon", R.mipmap.filefolder_kawayi);
            }
            list.add(map);
        }

        // 创建适配器对象
        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.item_file_explorer, new String[]{"filename", "icon"}, new int[]{R.id.item_tv, R.id.item_icon});
        fileLv.setAdapter(adapter);
        pathTv.setText("当前路径:" + currentParent.getAbsolutePath());
    }

    // 添加一个用于去除文件后缀的辅助方法
    private String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }


    // 定义一个文件排序比较器
    private class FileComparator implements Comparator<File> {
        private int sortOrder; // 0: 不排序, 1: 按照文件名排序, 2: 按照修改时间排序, 3: 按照文件大小排序

        FileComparator(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Override
        public int compare(File file1, File file2) {
            if (sortOrder == 0) {
                // 不排序
                return 0;
            } else if (sortOrder == 1) {
                // 按照文件名排序
                return file1.getName().compareToIgnoreCase(file2.getName());
            } else if (sortOrder == 2) {
                // 按照修改时间排序
                return Long.compare(file1.lastModified(), file2.lastModified());
            } else if (sortOrder == 3) {
                // 按照文件大小排序
                return Long.compare(file1.length(), file2.length());
            }

            return 0;
        }
    }

    //修改系统返回操作
    public void onBackPressed() {
        handleBackPressed(); // 在这里执行您定义的返回操作
    }
    private void handleBackPressed() {
        // 在这里执行您定义的返回操作
        // 例如，可以模拟点击返回按钮时的行为
        // 或者执行您想要的任何返回操作
        if (currentParent.getAbsolutePath().equals(root.getAbsolutePath())) {
            finish(); // 如果已经在根目录，则调用 finish() 来退出 Activity
        } else {
            currentParent = currentParent.getParentFile();
            currentFiles = currentParent.listFiles();
            inflateListView(currentFiles);
        }
    }
}

