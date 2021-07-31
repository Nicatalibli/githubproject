package com.test.screenrecord.folderpicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.test.screenrecord.R;
import com.test.screenrecord.common.Const;
import com.test.screenrecord.common.PrefUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FolderChooserDialog extends Dialog implements View.OnClickListener,
        DirectoryRecyclerAdapter.OnDirectoryClickedListerner, AdapterView.OnItemSelectedListener {

    public static OnDirectorySelectedListerner onDirectorySelectedListerner;
    private RecyclerView rv;
    private TextView tv_currentDir;
    private TextView tv_empty;
    public File currentDir;
    private ArrayList<File> directories;
    private AlertDialog dialog;
    private DirectoryRecyclerAdapter adapter;
    private Spinner spinner;
    private List<Storages> storages = new ArrayList<>();
    private boolean isExternalStorageSelected = false;
    private SharedPreferences prefs;

    public FolderChooserDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    private void initialize() {
        try {
            setContentView(R.layout.director_chooser);
            currentDir = new File(Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR);
            File[] SDCards = ContextCompat.getExternalFilesDirs(getContext().getApplicationContext(), null);
            storages.add(new Storages(Environment.getExternalStorageDirectory().getPath(), Storages.StorageType.Internal));
            prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//        if (SDCards.length > 1)
//            storages.add(new Storages(SDCards[1].getPath(), Storages.StorageType.External));
//        getRemovableSDPath(SDCards[1]);

            generateFoldersList();
            initView();
            initRecyclerView();
        } catch (Exception e) {
        }
    }

    private void initRecyclerView() {
        try {
            rv.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rv.setLayoutManager(layoutManager);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), layoutManager.getOrientation());
            rv.addItemDecoration(dividerItemDecoration);
            if (!isDirectoryEmpty()) {
                adapter = new DirectoryRecyclerAdapter(getContext(), this, directories);
                rv.setAdapter(adapter);
            }
            tv_currentDir.setText(currentDir.getPath());
        } catch (Exception e) {
        }
    }

    private boolean isDirectoryEmpty() {
        if (directories.isEmpty()) {
            rv.setVisibility(View.GONE);
            tv_empty.setVisibility(View.VISIBLE);
            return true;
        } else {
            rv.setVisibility(View.VISIBLE);
            tv_empty.setVisibility(View.GONE);
            return false;
        }
    }

    private void generateFoldersList() {
        try {
            File[] dir = currentDir.listFiles(new FolderChooserDialog.DirectoryFilter());
            directories = new ArrayList<>(Arrays.asList(dir));
            Collections.sort(directories, new FolderChooserDialog.SortFileName());
            Log.d(Const.TAG, "Directory size " + directories.size());
        } catch (Exception e) {
        }
    }

    private void initView() {
        try {
            Button btnOK, btnCancel;
            btnOK = findViewById(R.id.btn_ok);
            btnCancel = findViewById(R.id.btn_cancel);

            btnOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!currentDir.canWrite()) {
                        Toast.makeText(getContext(), "Cannot write to selected directory. Path will not be saved.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PrefUtils.saveStringValue(getContext(), getContext().getString(R.string.savelocation_key), currentDir.getPath());
                    onDirectorySelectedListerner.onDirectorySelected();
                    dismiss();
                }
            });

            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            ImageButton up = findViewById(R.id.nav_up);
            ImageButton createDir = findViewById(R.id.create_dir);
            tv_currentDir = findViewById(R.id.tv_selected_dir);
            rv = findViewById(R.id.rv);
            tv_empty = findViewById(R.id.tv_empty);
            spinner = findViewById(R.id.storageSpinner);
            up.setOnClickListener(this);
            createDir.setOnClickListener(this);
            ArrayList<String> StorageStrings = new ArrayList<>();
            for (Storages storage : storages) {
                String storageType = storage.getType() == Storages.StorageType.Internal ? "Internal Storage" :
                        "Removable Storage";
                StorageStrings.add(storageType);
            }
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, StorageStrings);

            // Drop down layout style - list view with radio button
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // attaching data adapter to spinner
            spinner.setAdapter(dataAdapter);
            spinner.setOnItemSelectedListener(this);
        } catch (Exception e) {
        }
    }

    private void changeDirectory(File file) {
        try {
            currentDir = file;
            Log.d(Const.TAG, "Changed dir is: " + file.getPath());
            generateFoldersList();
            if (!isDirectoryEmpty()) {
                adapter = new DirectoryRecyclerAdapter(getContext(), this, directories);
                rv.swapAdapter(adapter, true);
            }
            tv_currentDir.setText(currentDir.getPath());
        } catch (Exception e) {
        }
    }

    public void setCurrentDir(String currentDir) {
        try {
            File dir = new File(currentDir);
            if (dir.exists() && dir.isDirectory()) {
                this.currentDir = dir;
                Log.d(Const.TAG, "Directory set");
            } else {
                createFolder(dir.getPath());
                Log.d(Const.TAG, "Directory created");
            }
        } catch (Exception e) {
        }
    }

    public void setOnDirectoryClickedListerner(OnDirectorySelectedListerner onDirectoryClickedListerner) {
        FolderChooserDialog.onDirectorySelectedListerner = onDirectoryClickedListerner;
    }

    private void newDirDialog(Bundle savedState) {
        try {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = li.inflate(R.layout.directory_chooser_edit_text, null);
            final EditText input = view.findViewById(R.id.et_new_folder);
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (dialog != null) {
                        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setEnabled(!s.toString().trim().isEmpty());
                    }
                }
            });

            AlertDialog.Builder ab = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.alert_title_create_folder)
                    .setMessage(R.string.alert_message_create_folder)
                    .setView(view)
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    String dirName = input.getText().toString().trim();
                                    if (!dirName.isEmpty()) createFolder(dirName);
                                }
                            });

            dialog = ab.create();
            if (savedState != null) dialog.onRestoreInstanceState(savedState);
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!input.getText().toString().trim().isEmpty());
        } catch (Exception e) {
        }
    }

    private boolean createFolder(String dirName) {
        try {
            if (currentDir == null) {
                Toast.makeText(getContext(), "No directory selected", Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!currentDir.canWrite()) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_permission_make_dir), Toast.LENGTH_SHORT).show();
                return false;
            }

            File newDir;
            if (dirName.contains(Environment.getExternalStorageDirectory().getPath()))
                newDir = new File(dirName);
            else
                newDir = new File(currentDir, dirName);
            if (newDir.exists()) {
                Toast.makeText(getContext(), getContext().getString(R.string.dir_exist), Toast.LENGTH_SHORT).show();
                changeDirectory(new File(currentDir, dirName));
                return false;
            }

            if (!newDir.mkdir()) {
                Toast.makeText(getContext(), "Error creating directory", Toast.LENGTH_SHORT).show();
                Log.d(Const.TAG, newDir.getPath());
                return false;
            }

            changeDirectory(new File(currentDir, dirName));
        } catch (Exception e) {
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.nav_up:
                try {
                    File parentDirectory = new File(currentDir.getParent());
                    Log.d(Const.TAG, parentDirectory.getPath());
                    if (!isExternalStorageSelected) {
                        if (parentDirectory.getPath().contains(storages.get(0).getPath()))
                            changeDirectory(parentDirectory);
                    } else
                        changeExternalDirectory(parentDirectory);
                } catch (Exception e) {
                }
                return;
            case R.id.create_dir:
                newDirDialog(null);
                return;
        }
    }

    private void changeExternalDirectory(File parentDirectory) {
        try {
            String externalBaseDir = getRemovableSDPath(storages.get(1).getPath());
            if (parentDirectory.getPath().contains(externalBaseDir) && parentDirectory.canWrite())
                changeDirectory(parentDirectory);
            else if (parentDirectory.getPath().contains(externalBaseDir) && !parentDirectory.canWrite())
                Toast.makeText(getContext(), R.string.external_storage_dir_not_writable, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
        }
    }

    private String getRemovableSDPath(String pathSD) {
        //String pathSD = file.toString();
        int index = pathSD.indexOf("Android");
        Log.d(Const.TAG, "Short code is: " + pathSD.substring(0, index));
        String filename = pathSD.substring(0, index - 1);
        Log.d(Const.TAG, "External Base Dir " + filename);
        return filename;
    }

    @Override
    public void OnDirectoryClicked(File directory) {
        changeDirectory(directory);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d(Const.TAG, "Selected storage is: " + storages.get(i));
        isExternalStorageSelected = (storages.get(i).getType() == Storages.StorageType.External);
        if (isExternalStorageSelected && !prefs.getBoolean(Const.ALERT_EXTR_STORAGE_CB_KEY, false)) {
            showExtDirAlert();
        }
        changeDirectory(new File(storages.get(i).getPath()));
    }

    private void showExtDirAlert() {
        try {
            View checkBoxView = View.inflate(getContext(), R.layout.alert_checkbox, null);
            final CheckBox checkBox = checkBoxView.findViewById(R.id.donot_warn_cb);
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.alert_ext_dir_warning_title)
                    .setMessage(R.string.alert_ext_dir_warning_message)
                    .setView(checkBoxView)
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                if (checkBox.isChecked())
                                    prefs.edit().putBoolean(Const.ALERT_EXTR_STORAGE_CB_KEY, true).apply();
                            } catch (Exception e) {
                            }
                        }
                    })
                    .create().show();
        } catch (Exception e) {
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private class DirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !file.isHidden();
        }
    }

    //sorts based on the files name
    private class SortFileName implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    }
}
