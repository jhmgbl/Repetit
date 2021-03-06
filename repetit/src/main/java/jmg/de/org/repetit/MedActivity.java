package jmg.de.org.repetit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import jmg.de.org.repetit.lib.Path;
import jmg.de.org.repetit.lib.ProgressClass;
import jmg.de.org.repetit.lib.dbSqlite;
import jmg.de.org.repetit.lib.lib;
import me.texy.treeview.ContextMenuRecyclerView;
import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeView;

import static android.content.Context.MODE_PRIVATE;
import static jmg.de.org.repetit.lib.dbSqlite.getBedsQuery;
import static jmg.de.org.repetit.lib.lib.libString.MakeFitForQuery;
import static org.apache.commons.codec.binary.Base64.*;


public class MedActivity extends Fragment {
    public final static int fragID = 0;
    private static final String TAG = "MedActivity";
    private final int ID_MENU_SAVE = 0;

    public MainActivity _main;

    protected Toolbar toolbar;
    private ViewGroup viewGroup;
    private TreeNode rootMeds;
    public TreeView treeViewMeds;
    AppCompatEditText txtSearch;
    private ImageButton btnSearchAnd;
    private ImageButton btnSearchOr;
    String _lastQueryMedsMeds;
    private String[] _txt;
    private ArrayList<Integer> selectedMeds;
    private String[] finalArrSaves;
    private int finalCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _main = ((MainActivity) getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedinstancestate) {

        //initTreeView(view);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.med_menu, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        int saves = _main.getPreferences(MODE_PRIVATE).getInt("saves", 0);
        String strSaves = _main.getPreferences(MODE_PRIVATE).getString("strSaves", null);
        String[] arrSaves = (!lib.libString.IsNullOrEmpty(strSaves) ? strSaves.replaceAll("^\"|\"$", "").split("\";\"") : null);
        if (arrSaves == null) saves = 0;
        else saves = arrSaves.length;
        //String[] strSaves = _main.getPreferences(MODE_PRIVATE).getString("strSaves",null).split(",");
        MenuItem mnuResults = menu.findItem(R.id.mnu_results);
        SubMenu subResults = mnuResults.getSubMenu();
        subResults.clear();
        for (int i = 0; i < saves; i++) {
            if (menu.findItem(ID_MENU_SAVE + i * 5) != null) continue;
            SubMenu item =
                    subResults.addSubMenu(Menu.NONE, ID_MENU_SAVE + i * 5, Menu.NONE, arrSaves[i]);
            item.add(Menu.NONE, ID_MENU_SAVE + i * 5 + 1, Menu.NONE, R.string.open);
            item.add(Menu.NONE, ID_MENU_SAVE + i * 5 + 2, Menu.NONE, R.string.btnAdd);
            item.add(Menu.NONE, ID_MENU_SAVE + i * 5 + 3, Menu.NONE, R.string.rename);
            item.add(Menu.NONE, ID_MENU_SAVE + i * 5 + 4, Menu.NONE, R.string.delete);

            //MenuItemCompat.setActionView(item,new View(getContext()));
            //MenuItemCompat.getActionView(item).setOnLongClickListener(LongDeleteSave);
        }

        super.onPrepareOptionsMenu(menu);

    }

    private int finalSaves;
    private String finalStrSaves;
    private EditText input;
    /*private View.OnLongClickListener LongDeleteSave = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    MenuItem item = (MenuItem) v.getParent();
                    String title = (item.getTitle()).toString();
                    lib.yesnoundefined res = lib.ShowMessageYesNo(getContext(), String.format(getString(R.string.deletesave), title), getString(R.string.delete), false);
                    if (res == lib.yesnoundefined.yes) {
                        String strSaves = _main.getPreferences(MODE_PRIVATE).getString("strSaves", null);
                        strSaves = strSaves.replace(title, "").replace(";;", "").replaceAll("^;|;$", "");
                        _main.getPreferences(MODE_PRIVATE).edit().putString("strSaves", strSaves).commit();
                        int count = item.getItemId() - ID_MENU_SAVE;
                        _main.getPreferences(MODE_PRIVATE).edit().remove("save" + count).commit();
                    }
                    return true;
                }
            };


        */
    DialogInterface.OnClickListener ClickListenerSave = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            try {
                String txt = input.getText().toString();
                if (!lib.libString.IsNullOrEmpty(txt) && !lib.libString.IsNullOrEmpty(finalStrSaves) && finalArrSaves != null && Arrays.asList(finalArrSaves).contains(txt)) {
                    dialog.dismiss();
                    input = new EditText(getContext());
                    AlertDialog dlg = lib.getInputBox(getContext(), getString(R.string.nameexists, txt), getString(R.string.name), txt, false, ClickListenerSave, null, input);
                    dlg.show();
                } else {
                    int lSaves = finalSaves;
                    Bundle b = new Bundle();
                    onSaveInstanceState(b);
                    lSaves += 1;
                    Parcel p = Parcel.obtain(); // i make an empty one here, but you can use yours
                    b.writeToParcel(p, 0);
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] bytes = p.marshall();
                        bos.write(bytes, 0, bytes.length);
                        String data = new String(encodeBase64(bos.toByteArray()), "UTF-8");
                        _main.getPreferences(MODE_PRIVATE).edit().putString("save" + lSaves, data).commit();
                        bos.close();
                    } catch (Exception e) {
                        lib.ShowException(getContext(), e);
                        Log.e(getClass().getSimpleName(), e.toString(), e);
                    } finally {
                        p.recycle();
                    }

                    String lStrSaves = finalStrSaves;
                    if (!lib.libString.IsNullOrEmpty(lStrSaves)) lStrSaves += ";";
                    else lStrSaves = "";
                    lStrSaves += "\"" + txt + "\"";
                    _main.getPreferences(MODE_PRIVATE).edit().putInt("saves", lSaves).commit();
                    _main.getPreferences(MODE_PRIVATE).edit().putString("strSaves", lStrSaves).commit();
                }


            } catch (Throwable ex) {
                lib.ShowException(getContext(), ex);
            }
        }
    };

    DialogInterface.OnClickListener ClickListenerRename = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            try {
                String txt = input.getText().toString();
                if (!lib.libString.IsNullOrEmpty(txt) && !lib.libString.IsNullOrEmpty(finalStrSaves) && finalArrSaves != null && Arrays.asList(finalArrSaves).contains(txt)) {
                    dialog.dismiss();
                    input = new EditText(getContext());
                    AlertDialog dlg = lib.getInputBox(getContext(), getString(R.string.nameexists, txt), getString(R.string.name), txt, false, ClickListenerRename, null, input);
                    dlg.show();
                } else {
                    if (!lib.libString.IsNullOrEmpty(txt)) {
                        finalArrSaves[finalCount - 1] = txt;
                        String strSaves = lib.arrStrToCSV(finalArrSaves);
                        _main.getPreferences(MODE_PRIVATE).edit().putString("strSaves", strSaves).commit();
                    }
                }


            } catch (Throwable ex) {
                lib.ShowException(getContext(), ex);
            }
        }
    };

    private final static int OpenResultCode = 1001;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (_main == null || data == null) {
            lib.ShowMessage(getContext(), getString(R.string.noData), getString(R.string.noDataReceived));
            return;
        }
        if (requestCode == OpenResultCode && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                String file = uri.getPath(); // = lib.getRealFilePath(_main,uri);
                file = file.replace("/document/raw:", "");
                if (!new File(file).exists()) {
                    file = lib.getPath(_main, uri);
                }
                if (!lib.libString.IsNullOrEmpty(file)) {
                    if (_main.db != null) {
                        _main.db.close();
                        File f = new File(file);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                            f.setReadable(true);
                            f.setWritable(true);
                        }

                        if (f.exists() && f.canRead() && !f.canWrite()) {
                            String extPath = Environment.getExternalStorageDirectory().getPath();
                            File F = new File(extPath);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                                File F2 = _main.getExternalFilesDir(null);
                                if (F2 != null) F = F2;
                                extPath = F.getPath();
                                if (F.isDirectory() == false && !F.exists()) {
                                    F.mkdirs();
                                }
                            }
                            if (F.isDirectory() && F.exists()) {
                                String JMGDataDirectory = Path.combine(extPath, "repetit", "database");
                                File F1 = new File(Path.combine(JMGDataDirectory, f.getName()));
                                if (!F1.isDirectory() && !F1.exists()) {
                                    try {
                                        lib.copyFile(f.getPath(), Path.combine(JMGDataDirectory, f.getName()));
                                        f = F1;
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                        lib.ShowException(_main, e);
                                    }
                                }
                            }
                        } else {

                            String extPath = Environment.getExternalStorageDirectory().getPath();
                            File F = new File(extPath);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                                File F2 = _main.getExternalFilesDir(null);
                                if (F2 != null) F = F2;
                                extPath = F.getPath();
                                if (!F.isDirectory() && !F.exists()) {
                                    F.mkdirs();
                                }
                            }
                            if (F.isDirectory() && F.exists()) {
                                String JMGDataDirectory = Path.combine(extPath, "repetit", "database");
                                File F1 = new File(Path.combine(JMGDataDirectory, f.getName()));
                                if (!F1.isDirectory() && !F1.exists()) {
                                    try {
                                        lib.copyFile(_main, uri, Path.combine(JMGDataDirectory, f.getName()));
                                        f = F1;
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                        lib.ShowException(_main, e);
                                    }
                                }
                            }


                        }
                        boolean a = f.exists();
                        boolean b = f.canRead();
                        boolean c = f.canWrite();
                        if (a && b && c) {
                            _main.db.DB_PATH = f.getParent() + "/";
                            _main.db.dbname = f.getName();
                            _main.db.openDataBase();
                            if (_main.fPA.fragMed != null) _main.fPA.fragMed.refresh();
                            if (_main.fPA.fragSymptoms != null) _main.fPA.fragSymptoms.refresh();
                            if (_main.fPA.fragData != null) _main.fPA.fragData.refresh();
                            lib.ShowMessage(getContext(), getString(R.string.message), getString(R.string.databaseloaded));
                        } else if (!c && a && b) {
                            lib.ShowMessage(getContext(), getString(R.string.message), getString(R.string.filenotwritable));
                        } else {
                            lib.ShowMessage(getContext(), getString(R.string.message), getString(R.string.filecantbeopended));
                        }
                    }
                } else {
                    lib.ShowMessage(getContext(), getString(R.string.message), String.format(getString(R.string.FileNotFound), uri.toString()));
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                lib.ShowException(getContext(), throwable);
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int saves = _main.getPreferences(MODE_PRIVATE).getInt("saves", 0);
            String strSaves = _main.getPreferences(MODE_PRIVATE).getString("strSaves", null);
            String[] arrSaves;
            if (!lib.libString.IsNullOrEmpty(strSaves)) {
                arrSaves = strSaves.replaceAll("^\"|\"$", "").split("\";\"");
                saves = arrSaves.length;
            } else {
                arrSaves = null;
                saves = 0;
            }
            int ID = item.getItemId();
            finalSaves = saves;
            finalStrSaves = strSaves;
            finalArrSaves = arrSaves;
            switch (ID) {
                case R.id.mnu_save:
                    input = new EditText(getContext());
                    AlertDialog dlg = lib.getInputBox(getContext(), getString(R.string.save_result), getString(R.string.name), "", false, ClickListenerSave, null, input);
                    dlg.show();
                    return true;
                case R.id.mnu_open:
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.openfile)), OpenResultCode);
                    return true;
                default:
                    if (ID >= this.ID_MENU_SAVE && ID < this.ID_MENU_SAVE + saves * 5) {
                        try {
                            //ComplexPreferences prefs2 = ComplexPreferences.getComplexPreferences(getContext(), "save", MODE_PRIVATE);
                            int count = (ID - ID_MENU_SAVE) / 5 + 1;
                            int sub = (ID - ID_MENU_SAVE) % 5;
                            switch (sub) {
                                case 0:
                                    break;
                                case 1:
                                    String bytes = _main.getPreferences(MODE_PRIVATE).getString("save" + count, null);
                                    if (!lib.libString.IsNullOrEmpty(bytes)) {
                                        //b = prefs2.getObject("save" + count, Bundle.class);
                                        Parcel p = Parcel.obtain(); // i make an empty one here, but you can use yours
                                        Bundle b;
                                        try {
                                            byte[] data = decodeBase64(bytes.getBytes("UTF-8"));
                                            p.unmarshall(data, 0, data.length);
                                            p.setDataPosition(0);
                                            b = p.readBundle();
                                        } finally {
                                            p.recycle();
                                        }
                                        _lastQueryMedsMeds = b.getString("lastquerymeds");
                                        _main.lastQueryMedsMain = b.getString("lastquerymedsmain");
                                        _txt = b.getStringArray("txt");
                                        ArrayList<Integer> selMeds = b.getIntegerArrayList("selectedMeds");
                                        if (selMeds != null) selectedMeds = selMeds;
                                        if (b.containsKey("selectedSymp"))
                                            _main.selectedSymp = b.getIntegerArrayList("selectedSymp");
                                        if (!lib.libString.IsNullOrEmpty(_lastQueryMedsMeds)) {
                                            buildTreeRep(_lastQueryMedsMeds, true, _txt, selectedMeds, b);
                                        }
                                        //_main.selected = Selected;
                                    }
                                    return true;
                                case 2:
                                    bytes = _main.getPreferences(MODE_PRIVATE).getString("save" + count, null);
                                    if (!lib.libString.IsNullOrEmpty(bytes)) {
                                        //b = prefs2.getObject("save" + count, Bundle.class);
                                        Parcel p = Parcel.obtain(); // i make an empty one here, but you can use yours
                                        Bundle b;
                                        try {
                                            byte[] data = decodeBase64(bytes.getBytes("UTF-8"));
                                            p.unmarshall(data, 0, data.length);
                                            p.setDataPosition(0);
                                            b = p.readBundle();
                                        } finally {
                                            p.recycle();
                                        }
                                        _lastQueryMedsMeds = b.getString("lastquerymeds");
                                        String queryMedsMain = b.getString("lastquerymedsmain");
                                        _txt = b.getStringArray("txt");
                                        ArrayList<Integer> selMeds = b.getIntegerArrayList("selectedMeds");
                                        if (selMeds == null) selMeds = selectedMeds;
                                        if (selMeds.size() > 0) selectedMeds = selMeds;
                                        if (b.containsKey("selectedSymp"))
                                            _main.selectedSymp = b.getIntegerArrayList("selectedSymp");
                                        String[] qry;
                                        boolean blnAdd = true;
                                        if (_main.mPager.getCurrentItem() == SymptomsActivity.fragID) {
                                            _main.lastQueryMedsMain = queryMedsMain;
                                            qry = _main.fPA.fragSymptoms.getQueryMed(true, false, blnAdd, selMeds);
                                            _main.fPA.fragSymptoms.blnHasbeenRepertorised = true;
                                        } else if (_main.mPager.getCurrentItem() == MedActivity.fragID) {
                                            _main.lastQueryMedsMain = queryMedsMain;
                                            qry = _main.fPA.fragMed.getQueryMed(true, false, blnAdd, selMeds);
                                        } else {
                                            break;
                                        }
                                        selectedMeds = selMeds;
                                        String qrycombine = qry[1];
                                        if (lib.libString.IsNullOrEmpty(qrycombine)) break;
                                        _main.mPager.setCurrentItem(MedActivity.fragID);
                                        //String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                                        //        "WHERE " + qry[0] + " AND Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + qry[1] + ")";
                                        String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                                                "WHERE Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + qrycombine + ")";
                                        qryMedGrade += " ORDER BY Medikamente.Name, SymptomeOfMedikament.GRADE DESC";
                                        _main.fPA.fragMed._lastQueryMedsMeds = null;
                                        _main.fPA.fragMed.buildTreeRep(qryMedGrade, true, null, selMeds, b);
                                        //((MainActivity)getActivity()).fPA.fragMed.buildTree("SELECT * FROM Medikamente WHERE " + qry, true);
                                    }
                                    return true;
                                case 3:
                                    input = new EditText(getContext());
                                    finalCount = count;
                                    dlg = lib.getInputBox(getContext(), getString(R.string.rename_result), getString(R.string.name), arrSaves[count - 1], false, ClickListenerRename, null, input);
                                    dlg.show();
                                    return true;
                                case 4:
                                    lib.yesnoundefined res2 = lib.ShowMessageYesNo(getContext(), String.format(getString(R.string.deletesave), arrSaves[count - 1]), getString(R.string.delete), false);
                                    if (res2 == lib.yesnoundefined.yes) {
                                        try {
                                            arrSaves[count - 1] = null;//strSaves = strSaves.replace(arrSaves[count-1], "").replace("\"\"","").replace(";;", "").replaceAll("^;|;$", "");
                                            strSaves = lib.arrStrToCSV(arrSaves);
                                            SharedPreferences prefs = _main.getPreferences(MODE_PRIVATE);
                                            prefs.edit().putString("strSaves", strSaves).commit();
                                            prefs.edit().remove("save" + count).commit();
                                            for (int i = count; i < saves; i++) {
                                                prefs.edit().putString("save" + i, prefs.getString("save" + (i + 1), null)).commit();
                                            }
                                            if (count < saves) prefs.edit().remove("save" + saves);
                                            saves -= 1;
                                            _main.getPreferences(MODE_PRIVATE).edit().putInt("saves", saves).commit();
                                        } catch (Throwable ex) {
                                            lib.ShowException(getContext(), ex);
                                        }
                                    }
                                    return true;
                                default:
                                    throw new RuntimeException("invalid menu");
                            }
                        } catch (Throwable ex) {
                            lib.ShowException(getContext(), ex);
                        }
                    }
                    break;
            }
        } catch (Throwable ex) {
            lib.ShowException(getContext(), ex);
        }

        return super.onOptionsItemSelected(item);

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = _main.getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        try {
            ContextMenuRecyclerView.RecyclerViewContextMenuInfo info = (ContextMenuRecyclerView.RecyclerViewContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case R.id.cmnuSearch:
                    String search;
                    if (info.treeNode.getValue() instanceof TreeNodeHolderMed) {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) info.treeNode.getValue();
                        search = h.Name;
                    } else if (info.treeNode.getValue() instanceof TreeNodeHolderSympt) {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) info.treeNode.getValue();
                        search = h.SymptomText;
                    } else {
                        throw new RuntimeException("TreeNodeHolder not found!");
                    }
                    Uri uri = Uri.parse("http://www.google.com/#q=" + search);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                case R.id.cmnuShowAll:
                    //lib.ShowMessage(getContext(),((TreeNodeHolder)info.treeNode.getValue()).Text,"Node");
                    treeViewMeds.collapseNode(info.treeNode);
                    ArrayList<TreeNode> children = new ArrayList<>();
                    children.addAll(info.treeNode.getChildren());
                    info.treeNode.getChildren().clear();
                /*if (info.treeNode.getLevel() == 1)
                {
                    try
                    {
                        FirstLevelNodeViewBinderMed.buildTree(treeView,info.treeNode);
                    }
                    catch (Throwable throwable)
                    {
                        throwable.printStackTrace();
                    }
                }
                */
                    info.treeNode.holder.onNodeToggled(info.treeNode, true, children);
                    //treeView.toggleNode(info.treeNode);
                    //treeView.expandNode(info.treeNode);

                    return true;
                case R.id.cmnuAddTerm:
                    if (info.treeNode.getValue() instanceof TreeNodeHolderSympt) {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) info.treeNode.getValue();
                        search = h.ShortText;
                        Intent intent2 = new Intent(getContext(), ActivityTerms.class);
                        intent2.putExtra("term", search);
                        startActivity(intent2);
                        return true;
                    }
                case R.id.cmnuGetMeanings:
                    if (info.treeNode.getValue() instanceof TreeNodeHolderSympt) {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) info.treeNode.getValue();
                        search = h.ShortText;
                        if (_main.db != null) {
                            int FBID = _main.db.getTermID(search);
                            if (FBID > -1) {
                                Cursor c = _main.db.query("SELECT * FROM Bedeutungen WHERE FachbegriffsID = " + FBID);
                                if (c.moveToFirst()) {
                                    StringBuilder sb = new StringBuilder();
                                    do {
                                        if (sb.length() > 0) sb.append("\n");
                                        sb.append(c.getString(c.getColumnIndex("Text")));
                                    } while (c.moveToNext());
                                    lib.ShowMessage(getContext(), sb.toString(), search);
                                }
                                c.close();
                            }
                        }
                        return true;

                    }
                case R.id.cmnuTranslate:
                    if (info.treeNode.getValue() instanceof TreeNodeHolderSympt) {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) info.treeNode.getValue();
                        search = h.ShortText;
                        _main.translate("en", "de", search, null);
                        return true;
                    }
                case R.id.cmnuSearchGoogle:
                    search = null;
                    if (info.treeNode.getValue() instanceof TreeNodeHolderSympt) {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) info.treeNode.getValue();
                        search = h.ShortText;
                    } else if (info.treeNode.getValue() instanceof TreeNodeHolderMed) {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) info.treeNode.getValue();
                        search = h.Name;
                    }
                    try {
                        if (search != null) _main.searchGoogle(search);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return true;

                default:
                    return super.onContextItemSelected(item);
            }
        } catch (Throwable ex) {
            lib.ShowException(getContext(), ex);
            return super.onContextItemSelected(item);
        }
    }

    public void initTreeView(View view, Bundle savedinstancestate) throws Throwable {
        if (view != null) initView(view);

        rootMeds = TreeNode.root();
        treeViewMeds = new TreeView(rootMeds, _main, new MyNodeViewFactoryMed());
        View view2 = treeViewMeds.getView();
        registerForContextMenu(view2);
        view2.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        viewGroup.addView(view2);
        if (_main.treeView == null) _main.treeView = treeViewMeds;

        if (lib.libString.IsNullOrEmpty(_lastQueryMedsMeds) && !lib.libString.IsNullOrEmpty(_main.lastQueryMedsMain)) {
            String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                    "WHERE Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + _main.lastQueryMedsMain + ")";
            qryMedGrade += " ORDER BY Medikamente.Name, SymptomeOfMedikament.GRADE DESC";
            buildTreeRep(qryMedGrade, true, null, selectedMeds, savedinstancestate);
        } else if (!lib.libString.IsNullOrEmpty(_lastQueryMedsMeds)) {
            buildTreeRep(_lastQueryMedsMeds, true, _txt, selectedMeds, savedinstancestate);
        } else {
            buildTree("SELECT * FROM Medikamente ORDER BY Name", true, savedinstancestate);
        }


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedinstancestate) {
        View v = null;
        try {
            v = inflater.inflate(R.layout.activity_med, container, false);
            txtSearch = (AppCompatEditText) v.findViewById(R.id.txtSearch);
            txtSearch.setImeOptions(EditorInfo.IME_ACTION_DONE);
            txtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    String txt = txtSearch.getText().toString();
                    if (!lib.libString.IsNullOrEmpty(txt)) try {
                        searchSymptoms(txt, true);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    return true;
                }
            });
            btnSearchAnd = (ImageButton) v.findViewById(R.id.btnSearchAnd);
            btnSearchAnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String txt = txtSearch.getText().toString();
                    if (!lib.libString.IsNullOrEmpty(txt)) try {
                        searchSymptoms(txt, true);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            });
            btnSearchOr = (ImageButton) v.findViewById(R.id.btnSearchOr);
            btnSearchOr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String txt = txtSearch.getText().toString();
                    if (!lib.libString.IsNullOrEmpty(txt)) try {
                        searchSymptoms(txt, false);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            });
            if (savedinstancestate != null) {
                _lastQueryMedsMeds = savedinstancestate.getString("lastquerymeds");
                selectedMeds = savedinstancestate.getIntegerArrayList("selectedMeds");
            }
            initTreeView(v, savedinstancestate);
            return v;
        } catch (Throwable ex) {
            lib.ShowException(_main, ex);
            return v;
        }
    }

    private void restoreTreeView(Bundle savedinstancestate) throws Throwable {
        if (treeViewMeds != null && savedinstancestate != null) {
            if (rootMeds != null) {
                ArrayList<Integer> expMed = savedinstancestate.getIntegerArrayList("expMed");
                ArrayList<Integer> expMedSymp = savedinstancestate.getIntegerArrayList("expMedSymp");
                if (expMed.size() == 0) return;
                for (TreeNode t : rootMeds.getChildren()) {
                    if (t.hasChild() == false || true) {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) t.getValue();
                        if (expMed.contains(h.ID)) {
                            expMed.remove(new Integer(h.ID));
                            while (expMedSymp.size() > 0 && expMedSymp.get(0) == -99)
                                expMedSymp.remove(0);
                            if (t.hasChild() == false)
                                FirstLevelNodeViewBinderMed.buildTree(treeViewMeds, t, null);
                            else treeViewMeds.expandNode(t);
                            lib.gStatus = "expSympMed";
                            expSympMed(h.ID, t, expMedSymp, selectedMeds);
                        }
                        if (expMed.size() == 0) break;

                    }
                }
                if (selectedMeds != null && selectedMeds.size() > 0) {
                    for (TreeNode t : rootMeds.getChildren()) {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) t.getValue();
                        while (selectedMeds.size() > 0 && h.ID == selectedMeds.get(0)) {
                            selectedMeds.remove(0);
                            SymptomsActivity.AddNodesRecursive(_main, 1, null, t, selectedMeds.get(0), selectedMeds.get(1), h.ID);
                            selectedMeds.remove(0);
                            selectedMeds.remove(0);
                        }
                    }
                }

            }

        }
    }

    private void expSympMed(int id, TreeNode t, ArrayList<Integer> expMedSymp, ArrayList<Integer> selected) throws Throwable {

        CheckSelected(id, t, selected);
        if (expMedSymp.size() == 0) return;
        if (-99 == expMedSymp.get(0) && -99 == expMedSymp.get(1)) {
            expMedSymp.remove(0);
            expMedSymp.remove(0);
            return;
        }


        for (TreeNode tt : t.getChildren()) {
            if (expMedSymp.size() == 0) return;

            if (-99 == expMedSymp.get(0) && -99 == expMedSymp.get(1)) {
                expMedSymp.remove(0);
                expMedSymp.remove(0);
                break;
            }
            TreeNodeHolderSympt h = (TreeNodeHolderSympt) tt.getValue();
            if (h.ParentMedID == expMedSymp.get(0) && h.ID == expMedSymp.get(1)) {
                expMedSymp.remove(0);
                expMedSymp.remove(0);
                if (tt.hasChild() == false)
                    SecondLevelNodeViewBinder.buildTree(treeViewMeds, tt, null);
                else treeViewMeds.expandNode(tt);
                if (expMedSymp.size() <= 0) {
                    CheckSelected(id, tt, selected);
                    break;
                }
                if (-99 == expMedSymp.get(0) && -99 == expMedSymp.get(1)) {
                    expMedSymp.remove(0);
                    expMedSymp.remove(0);
                    CheckSelected(id, tt, selected);
                } else {
                    expSympMed(id, tt, expMedSymp, selected);
                }

            }
        }
    }

    private void CheckSelected(int id, TreeNode t, ArrayList<Integer> selected) {
        if (selected == null) return;
        for (TreeNode tt : t.getChildren()) {
            if (selected.size() <= 0) break;
            TreeNodeHolderSympt h = (TreeNodeHolderSympt) tt.getValue();
            for (int i = 0; i < selected.size(); i += 3) {
                if (selected.get(i) == id && selected.get(i + 1) == h.ID) {
                    treeViewMeds.selectNode(tt, 1);
                    tt.setWeight(selected.get(i + 2));
                    selected.remove(i);
                    selected.remove(i);
                    selected.remove(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastquerymeds", _lastQueryMedsMeds);
        outState.putString("lastquerymedsmain", _main.lastQueryMedsMain);
        outState.putStringArray("txt", _txt);
        outState.putIntegerArrayList("selectedSymp", _main.selectedSymp);
        if (_main.db != null) {
            outState.putString("dbname", _main.db.dbname);
            outState.putString("dbpath", _main.db.DB_PATH);
        }
        if (treeViewMeds != null) {
            if (rootMeds != null) {
                ArrayList<Integer> expMed = new ArrayList<>();
                ArrayList<Integer> expMedSymp = new ArrayList<>();
                ArrayList<Integer> Selected = new ArrayList<>();
                for (TreeNode t : rootMeds.getChildren()) {
                    if (t.hasChild() && t.isExpanded()) {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) t.getValue();
                        expMed.add(h.ID);
                        getSympMed(h.ID, t, expMedSymp);
                    }
                }
                for (TreeNode t : treeViewMeds.getSelectedNodes()) {
                    if (t.getValue() instanceof TreeNodeHolderSympt) {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) t.getValue();
                        Selected.add(h.ParentMedID);
                        Selected.add(h.ID);
                        Selected.add(t.getWeight());
                        //Selected.add(root.getWeight());
                    }
                }
                outState.putIntegerArrayList("expMed", expMed);
                outState.putIntegerArrayList("expMedSymp", expMedSymp);
                outState.putIntegerArrayList("selectedMeds", Selected);
            }

        }
    }

    private void getSympMed(int Medid, TreeNode t, ArrayList<Integer> sympMed) {
        boolean hasChild = false;
        for (TreeNode tt : t.getChildren()) {
            if (tt.hasChild() && tt.isExpanded()) {
                TreeNodeHolderSympt h = (TreeNodeHolderSympt) tt.getValue();
                sympMed.add(Medid);
                sympMed.add(h.ID);
                getSympMed(Medid, tt, sympMed);
                hasChild = true;
            } else {

            }
        }
        if (hasChild) {
            sympMed.add(-99);
            sympMed.add(-99);
        }

    }

    public static String getWhereWhole(String column, String search) {
        return "WHERE " + column + " like '% " + search + " %' OR " + column + " like '" + search + " %' OR " + column + " like '% " + search + "' OR " + column + " like '" + search + "'";
    }

    private void searchSymptoms(String searchtxt, final boolean AndFlag) throws Throwable {
        if (lib.libString.IsNullOrEmpty(searchtxt)) return;
        String[] txt = searchtxt.split("\\.");
        if (txt.length <= 1) txt = searchtxt.split("\\s+");
        if (txt.length > 1 && AndFlag) {
            final String[] ftxt = txt;
            lib.getMessagePosNeg(getContext(), getString(R.string.WideOrNarrow), getString(R.string.search), getString(R.string.wide)
                    , getString(R.string.narrow), false, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            searchSymptoms2(ftxt, AndFlag, true);

                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            searchSymptoms2(ftxt, AndFlag, false);
                        }
                    }).show();
        } else {
            searchSymptoms2(txt, AndFlag, false);
        }
    }

    private void searchSymptoms2(String[] txt, boolean AndFlag, boolean blnWide) {
        try {
            //String qry = "SELECT Medikamente.* FROM Symptome WHERE ";
            if (!lib.libString.IsNullOrEmpty(_main.lastQueryMedsMain)) {
                lib.yesnoundefined res = (lib.ShowMessageYesNo(getContext(), getString(R.string.alreadysearched), getString(R.string.continuesearch), false));
                if (res != lib.yesnoundefined.yes) return;
            }
            String where = "";
            String whereSympt = "";
            String Bed[] = null;
            ArrayList<String> BedAll = new ArrayList<>();
            String combinedSearch = null;
            for (int i = 0; i < txt.length; i++) {
                String s = txt[i];
                if (s.startsWith("(") || combinedSearch != null) {
                    if (s.startsWith("(")) txt[i] = s.substring(1);
                    if (combinedSearch == null) combinedSearch = "";
                    combinedSearch += s;
                    if (!combinedSearch.endsWith(")")) {
                        combinedSearch += ".";
                        continue;
                    }
                    txt[i] = s.substring(0,s.length()-1);
                } else {
                    combinedSearch = null;
                }
                if (!lib.libString.IsNullOrEmpty(s)) {
                    s = MakeFitForQuery(s, true);
                    if (_main.blnSearchTerms && _main.db != null) {
                        Bed = _main.db.getFachbegriffe(s);
                        if (Bed != null) for (String ss : Bed) BedAll.add(ss);
                    }
                    if (AndFlag) {
                        if (!(where.equalsIgnoreCase(""))) where += " AND ";
                        if (!(whereSympt.equalsIgnoreCase(""))) whereSympt += " OR ";
                        String whereS = "";
                        if (txt.length > 1) {
                            //whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.Text", s) : "WHERE Symptome.Text LIKE '%" + s + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.Text", Bed) : ""));
                            if (blnWide) {
                                if (combinedSearch != null) {
                                    combinedSearch = combinedSearch.substring(1, combinedSearch.length() - 1);
                                    boolean blnOr = false;
                                    String[] txt2 = null;
                                    if (combinedSearch.toLowerCase().contains(".or.") || combinedSearch.contains("|"))
                                    {
                                        txt2 = combinedSearch.split("(?i)\\.or\\.");
                                        if (txt2.length <= 1) txt2 = combinedSearch.split("\\|");
                                        blnOr = true;
                                    }
                                    else {
                                        txt2 = combinedSearch.split("\\.");
                                        if (txt2.length <= 1) txt2 = combinedSearch.split("\\s+");
                                    }
                                    String wheretmp = "";
                                    boolean isSecond = false;
                                    for (String ss : txt2) {
                                        ss = MakeFitForQuery(ss, true);
                                        if (_main.blnSearchTerms && _main.db != null) {
                                            Bed = _main.db.getFachbegriffe(ss);
                                            if (Bed != null) for (String sss : Bed) BedAll.add(sss);
                                        }
                                        if (blnOr)
                                        {
                                            whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.ShortText", ss) : "WHERE Symptome.ShortText LIKE '%" + ss + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.ShortText", Bed) : ""));
                                        }
                                        else
                                        {
                                            whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.Text", ss) : "WHERE Symptome.Text LIKE '%" + ss + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.Text", Bed) : ""));
                                        }
                                        if (!(wheretmp.equalsIgnoreCase(""))) {
                                            if (blnOr)
                                            {
                                                wheretmp += " OR ";
                                            }
                                            else
                                            {
                                                wheretmp += " AND ";
                                            }
                                        }
                                        wheretmp += whereS.substring(6);
                                        isSecond = true;
                                    }
                                    combinedSearch = null;
                                    whereS = "WHERE " + wheretmp;
                                } else {
                                    whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.ShortText", s) : "WHERE Symptome.ShortText LIKE '%" + s + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.ShortText", Bed) : ""));
                                }
                                where += "Medikamente.ID IN (Select SymptomeOfMedikament.MedikamentID FROM SymptomeOfMedikament WHERE SymptomeOfMedikament.SymptomID IN (SELECT Symptome.ID FROM Symptome AS Symptome " + whereS + "))";
                                whereS = whereS.substring(6).replace("Symptome.", "Symptome.");
                                whereS = "(" + whereS + ")";
                                whereSympt += whereS;
                            } else {
                                if (combinedSearch != null) {
                                    combinedSearch = combinedSearch.substring(1, combinedSearch.length() - 1);
                                    boolean blnOr = false;
                                    String[] txt2 = null;
                                    if (combinedSearch.toLowerCase().contains(".or.") || combinedSearch.contains("|"))
                                    {
                                        txt2 = combinedSearch.split("(?i)\\.or\\.");
                                        if (txt2.length <= 1) txt2 = combinedSearch.split("\\|");
                                        blnOr = true;
                                    }
                                    else {
                                        txt2 = combinedSearch.split("\\.");
                                        if (txt2.length <= 1) txt2 = combinedSearch.split("\\s+");
                                    }
                                    String wheretmp = "";
                                    boolean isSecond = false;
                                    for (String ss : txt2) {
                                        ss = MakeFitForQuery(ss, true);
                                        if (_main.blnSearchTerms && _main.db != null) {
                                            Bed = _main.db.getFachbegriffe(ss);
                                            if (Bed != null) for (String sss : Bed) BedAll.add(sss);
                                        }
                                        if (blnOr)
                                        {
                                            whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.ShortText", ss) : "WHERE Symptome.ShortText LIKE '%" + ss + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.ShortText", Bed) : ""));
                                        }
                                        else
                                        {
                                            whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.Text", ss) : "WHERE Symptome.Text LIKE '%" + ss + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.Text", Bed) : ""));
                                        }
                                        if (!(wheretmp.equalsIgnoreCase(""))) {
                                            if (blnOr)
                                            {
                                                wheretmp += " OR ";
                                            }
                                            else
                                            {
                                                wheretmp += " AND ";
                                            }
                                        }
                                        wheretmp += whereS.substring(6);
                                        isSecond = true;
                                    }
                                    combinedSearch = null;
                                    whereS = "WHERE " + wheretmp;
                                } else {
                                    whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.Text", s) : "WHERE Symptome.Text LIKE '%" + s + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.Text", Bed) : ""));
                                }
                                where += whereS.substring(6);
                                //where += "SymptomeOfMedikament.SymptomID IN (SELECT Symptome.ID FROM Symptome "  + whereS + ")";
                            }
                        } else {
                            whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.ShortText", s) : "WHERE Symptome.ShortText LIKE '%" + s + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.ShortText", Bed) : ""));
                            //where += "Medikamente.ID IN (Select S2.MedikamentID FROM SymptomeOfMedikament AS S2 WHERE S2.SymptomID IN (SELECT Symptome.ID FROM Symptome AS Symptome "  + whereS + "))";
                            where += whereS.substring(6);
                            //where += "SymptomeOfMedikament.SymptomID IN (SELECT Symptome.ID FROM Symptome "  + whereS + ")";
                        }
                        //whereS = whereS.substring(6).replace("Symptome.","Symptome.");
                        //whereSympt += whereS;
                    } else {
                        if (!(where.equalsIgnoreCase(""))) where += " OR ";
                        String whereS = "";
                        if (combinedSearch != null) {
                            combinedSearch = combinedSearch.substring(1, combinedSearch.length() - 1);
                            boolean blnOr = false;
                            String[] txt2 = null;
                            if (combinedSearch.toLowerCase().contains(".or.") || combinedSearch.contains("|"))
                            {
                                txt2 = combinedSearch.split("(?i)\\.or\\.");
                                if (txt2.length <= 1) txt2 = combinedSearch.split("\\|");
                                blnOr = true;
                            }
                            else {
                                txt2 = combinedSearch.split("\\.");
                                if (txt2.length <= 1) txt2 = combinedSearch.split("\\s+");
                            }
                            String wheretmp = "";
                            boolean isSecond = false;
                            for (String ss : txt2) {
                                ss = MakeFitForQuery(ss, true);
                                if (_main.blnSearchTerms && _main.db != null) {
                                    Bed = _main.db.getFachbegriffe(ss);
                                    if (Bed != null) for (String sss : Bed) BedAll.add(sss);
                                }
                                if (blnOr)
                                {
                                    whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.ShortText", ss) : "WHERE Symptome.ShortText LIKE '%" + ss + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.ShortText", Bed) : ""));
                                }
                                else
                                {
                                    whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.Text", ss) : "WHERE Symptome.Text LIKE '%" + ss + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.Text", Bed) : ""));
                                }
                                if (!(wheretmp.equalsIgnoreCase(""))) {
                                    if (blnOr)
                                    {
                                        wheretmp += " OR ";
                                    }
                                    else
                                    {
                                        wheretmp += " AND ";
                                    }
                                }
                                wheretmp += whereS.substring(6);
                                isSecond = true;
                            }
                            combinedSearch = null;
                            whereS = "WHERE (" + wheretmp + ")";
                        } else {
                            whereS = (_main.blnSearchWholeWord ? getWhereWhole("Symptome.Text", s) : "WHERE Symptome.Text LIKE '%" + s + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.Text", Bed) : ""));
                        }

                        where += whereS.substring(6); //((_main.blnSearchWholeWord ? getWhereWhole("Symptome.ShortText", s) : "WHERE Symptome.ShortText LIKE '%" + MakeFitForQuery(s, true) + "%'" + (_main.blnSearchTerms ? getBedsQuery("Symptome.ShortText", Bed) : "")) + "").substring(6);
                    }
                }
            }
            if (!AndFlag || txt.length < 2) txt = null;
            //AddSymptomeQueryRecursive(root,qry,-1,true);
            String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                    "WHERE Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + where + ")" + (whereSympt.length() > 0 ? " AND (" + whereSympt + ")" : "");
            qryMedGrade += " ORDER BY Medikamente.Name, SymptomeOfMedikament.GRADE DESC";
            buildTreeRep(qryMedGrade, true, txt, BedAll, null, null);
            _lastQueryMedsMeds = qryMedGrade;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }


    public String getSelectedNodes() {
        StringBuilder stringBuilder = new StringBuilder("You have selected: ");
        List<TreeNode> selectedNodes = treeViewMeds.getSelectedNodes();
        for (int i = 0; i < selectedNodes.size(); i++) {
            if (i < 5) {
                stringBuilder.append(selectedNodes.get(i).getValue().toString()).append(",");
            } else {
                stringBuilder.append("...and ").append(selectedNodes.size() - 5).append(" more.");
                break;
            }
        }
        return stringBuilder.toString();
    }

    public void refresh() throws Throwable {
        _lastQueryMedsMeds = null;
        buildTree("SELECT * FROM Medikamente ORDER BY Name", true, null);
    }


    public class TreeNodeHolderMed extends TreeNodeHolder {
        public final int ID;
        public final String Name;
        public final String Beschreibung;
        public int totalGrade;
        public int count;

        public TreeNodeHolderMed(MainActivity context, int level, String Text, String path, int ID, String Name, String Beschreibung) {
            super(level, Text, path, context);
            this.ID = ID;
            this.Name = Name;
            this.Beschreibung = Beschreibung;
        }
    }


    public void buildTree(final String qry, final boolean refresh, final Bundle savedinstancestate) {
        final Context context = getContext();
        if (savedinstancestate != null && _main != null && _main.db != null) {
            String dbname = savedinstancestate.getString("dbname");
            String dbpath = savedinstancestate.getString("dbpath");
            if (dbname != null && dbpath != null && (!dbname.equalsIgnoreCase(_main.db.dbname) || !dbpath.equalsIgnoreCase(_main.db.DB_PATH))) {
                _main.db.close();
                _main.db.dbname = dbname;
                _main.db.DB_PATH = dbpath;
                _main.db.openDataBase();
            }
        }
        new AsyncTask<Void, ProgressClass, Integer>() {
            public boolean cancelled;
            public Throwable ex;
            public int counter;
            public int oldmax;
            public String oldmsg;
            ProgressDialog pd;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                createProgress();
            }

            private void createProgress() {
                pd = new ProgressDialog(context);
                pd.setTitle(getString(R.string.repertorising));
                pd.setMessage(getString(R.string.startingRep));
                pd.setIndeterminate(false);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setCancelable(true);
                pd.setCanceledOnTouchOutside(true);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelled = true;
                    }
                });
                pd.show();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    ProgressClass pc = new ProgressClass(0, 100, context.getString(R.string.startingquery), false);
                    publishProgress(pc);
                    if (rootMeds.getChildren().size() > 0) {
                        List<TreeNode> l = rootMeds.getChildren();
                        l.clear();
                        rootMeds.setChildren(l);
                    }
                    dbSqlite db = ((MainActivity) getActivity()).db;
                    try {

                        Cursor c = db.query(qry);
                        try {
                            if (c.moveToFirst()) {
                                final int ColumnNameId = c.getColumnIndex("Name");
                                final int ColumnIDId = c.getColumnIndex("ID");
                                final int ColumnBeschreibungId = c.getColumnIndex("Beschreibung");
                                final int ColumnPolychrest = c.getColumnIndex("Polychrest");
                                int count = c.getCount();
                                do {
                                    counter += 1;
                                    if (count < 10 || counter % (count / 10) == 0) {
                                        pc.update(counter, count, context.getString(R.string.processingquery), false);
                                        publishProgress(pc);
                                    }
                                    int ID = c.getInt(ColumnIDId);
                                    String Name = c.getString(ColumnNameId);
                                    String Beschreibung = c.getString(ColumnBeschreibungId);
                                    Boolean Polychrest = c.isNull(ColumnPolychrest) ? false : c.getInt(ColumnPolychrest) != 0;
                                    TreeNode treeNode = new TreeNode(new TreeNodeHolderMed((MainActivity) getActivity(), 0, (Polychrest ? "!" : "") + Name, "Med" + ID, ID, Name, Beschreibung));
                                    treeNode.setLevel(0);
                                    rootMeds.addChild(treeNode);
                                    if (cancelled) break;
                                } while (c.moveToNext());
                            }
                        } finally {
                            c.close();
                        }

                    } catch (Throwable ex) {
                        this.ex = ex;
                    } finally {
                        db.close();
                    }
                } catch (Throwable ex) {
                    this.ex = ex;
                }

                return counter;

            }

            @Override
            protected void onProgressUpdate(ProgressClass... params) {
                try {
                    super.onProgressUpdate(params);
                    ProgressClass p = params[0];
                    if (pd != null) {
                        if (p.blnRestart) {
                            pd.dismiss();
                            createProgress();
                            pd.show();
                        }
                        pd.setProgress(p.counter);
                        if (p.msg != null && !p.msg.equalsIgnoreCase(oldmsg)) {
                            pd.setMessage(p.msg);
                            oldmsg = p.msg;
                        }
                        if (p.max > 0 && !(p.max == oldmax)) {
                            pd.setMax(p.max);
                            oldmax = p.max;
                        }
                    } else {
                        Log.i("dbsqlite", "no progress");
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            protected void onPostExecute(final Integer result) {
                // continue what you are doing...
                try {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    if (refresh && treeViewMeds != null) treeViewMeds.refreshTreeView();
                    if (savedinstancestate != null) try {
                        restoreTreeView(savedinstancestate);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    if (this.ex != null) lib.ShowException(context, ex);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }


        }.execute();
    }


    public void buildTreeRep(final String qry, final boolean refresh, final String[] txt, final ArrayList<Integer> selected, final Bundle savedinstancestate) {
        buildTreeRep(qry, refresh, txt, null, selected, savedinstancestate);
    }


    public void buildTreeRep(final String qry, final boolean refresh, final String[] txt, final ArrayList<String> Bed, final ArrayList<Integer> selected, final Bundle savedinstancestate) {
        final Context context = getContext();
        if (savedinstancestate != null && _main != null && _main.db != null) {
            String dbname = savedinstancestate.getString("dbname");
            String dbpath = savedinstancestate.getString("dbpath");
            if (dbname != null && dbpath != null && (!dbname.equalsIgnoreCase(_main.db.dbname) || !dbpath.equalsIgnoreCase(_main.db.DB_PATH))) {
                _main.db.close();
                _main.db.dbname = dbname;
                _main.db.DB_PATH = dbpath;
                _main.db.openDataBase();
            }
        }
        new AsyncTask<Void, ProgressClass, Integer>() {
            public boolean blnQry;
            public boolean cancelled;
            public Throwable ex;
            public int oldmax;
            public String oldmsg;
            ProgressDialog pd;

            public void cancel() {
                this.cancelled = true;
                if (blnQry) this.cancel(true);
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                createProgress();
            }

            private void createProgress() {
                pd = new ProgressDialog(context);
                pd.setTitle(getString(R.string.repertorising));
                pd.setMessage(getString(R.string.startingRep));
                pd.setIndeterminate(false);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setCancelable(true);
                pd.setCanceledOnTouchOutside(true);
                pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel();
                    }
                });
                pd.show();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                int counter = 0;
                try {
                    ProgressClass pc = new ProgressClass(0, 100, context.getString(R.string.startingRep), false);
                    publishProgress(pc);
                    boolean Initialized = true;
                    final String CodeLoc = TAG + ".initTreeview";
                    lib.gStatus = CodeLoc + " Start";
                    int MedID;
                    //ArrayList<TreeNodeHolderMed> arrMed = new ArrayList<>();
                    if (rootMeds.getChildren().size() > 0) {
                        List<TreeNode> l = rootMeds.getChildren();
                        l.clear();
                        rootMeds.setChildren(l);
                    }
                    dbSqlite db = ((MainActivity) getActivity()).db;
                    try {
                        synchronized (MedActivity.this) {
                            blnQry = true;
                        }
                        Cursor c = db.query(qry);
                        synchronized (MedActivity.this) {
                            blnQry = false;
                        }
                        try {
                            if (c.moveToFirst()) {
                                int ColumnNameId = c.getColumnIndex("Name");
                                int ColumnIDId = c.getColumnIndex("ID");
                                int ColumnBeschreibungId = c.getColumnIndex("Beschreibung");
                                int ColumnGrade = c.getColumnIndex("Grade");
                                int ColumnPolychrest = c.getColumnIndex("Polychrest");
                                pc.update(0, c.getCount(), context.getString(R.string.processingquery), false);
                                this.publishProgress(pc);
                                counter += 1;
                                pc.counter = counter;
                                this.publishProgress(pc);
                                do {
                                    //if(!c.moveToNext()) break;
                                    int ID = c.getInt(ColumnIDId);
                                    String Name = c.getString(ColumnNameId);
                                    String Beschreibung = c.getString(ColumnBeschreibungId);
                                    int sum = 0;
                                    int nexts = 0;
                                    Boolean Polychrest = c.isNull(ColumnPolychrest) ? false : c.getInt(ColumnPolychrest) != 0;
                                    TreeNodeHolderMed hMed = new TreeNodeHolderMed((MainActivity) getActivity(), 0, (Polychrest ? "!" : "") + Name, "Med" + ID, ID, Name, Beschreibung);
                                    TreeNode treeNode = new TreeNode(hMed);
                                    treeNode.setLevel(0);
                                    rootMeds.addChild(treeNode);
                                    int f = insertSymptom(c, treeNode, hMed, selected, ID, txt, Bed);
                                    if (f == -2) f = 1;
                                    if (f >= 0) {
                                        sum = c.getInt(ColumnGrade) * f;
                                        nexts += 1;
                                    }
                                    while (c.moveToNext() && c.getInt(ColumnIDId) == ID) {
                                        counter += 1;
                                        if (pc.max < 10 || counter % (pc.max / 10) == 0) {
                                            pc.counter = counter;
                                            this.publishProgress(pc);
                                        }
                                        f = insertSymptom(c, treeNode, hMed, selected, ID, txt, Bed);
                                        if (f == -2) f = 1;
                                        if (f >= 0) {
                                            nexts += 1;
                                            sum += c.getInt(ColumnGrade) * f;
                                        }
                                        if (cancelled) break;
                                    }
                                    counter += 1;
                                    hMed.totalGrade = sum;
                                    hMed.count = nexts;
                                    hMed.Text += "(" + hMed.totalGrade + "/" + hMed.count + ")";
                                    if (cancelled) break;
                                } while (!c.isAfterLast());
                                List<TreeNode> l = rootMeds.getChildren();

                                Collections.sort(l, new Comparator<TreeNode>() {
                                    @Override
                                    public int compare(TreeNode lhs, TreeNode rhs) {
                                        TreeNodeHolderMed h1 = (TreeNodeHolderMed) lhs.getValue();
                                        TreeNodeHolderMed h2 = (TreeNodeHolderMed) rhs.getValue();
                                        if (h1.totalGrade > h2.totalGrade) return -1;
                                        if (h1.totalGrade == h2.totalGrade && h1.count > h2.count)
                                            return -1;
                                        if (h1.totalGrade == h2.totalGrade && h1.count == h2.count)
                                            return 0;
                                        return 1;
                                    }
                                });
                                rootMeds.setChildren(l);
                                //if (refresh) treeView.refreshTreeView();

                            }
                        } finally {
                            c.close();
                        }

                    } catch (Throwable ex) {
                        this.ex = ex;
                    } finally {
                        db.close();
                    }
                } catch (Throwable ex) {
                    this.ex = ex;
                }
                return counter;

            }

            @Override
            protected void onProgressUpdate(ProgressClass... params) {
                try {
                    super.onProgressUpdate(params);
                    ProgressClass p = params[0];
                    if (pd != null) {
                        if (p.blnRestart) {
                            pd.dismiss();
                            createProgress();
                            pd.show();
                        }
                        pd.setProgress(p.counter);
                        if (p.msg != null && !p.msg.equalsIgnoreCase(oldmsg)) {
                            pd.setMessage(p.msg);
                            oldmsg = p.msg;
                        }
                        if (p.max > 0 && !(p.max == oldmax)) {
                            pd.setMax(p.max);
                            oldmax = p.max;
                        }
                    } else {
                        Log.i("dbsqlite", "no progress");
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            protected void onPostExecute(final Integer result) {
                // continue what you are doing...
                try {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    if (refresh && treeViewMeds != null) treeViewMeds.refreshTreeView();
                    _lastQueryMedsMeds = qry;
                    _txt = txt;
                    if (savedinstancestate != null) try {
                        restoreTreeView(savedinstancestate);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    if (this.ex != null) lib.ShowException(context, ex);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }


        }.execute();


    }

    private int insertSymptom(Cursor c, TreeNode treeNode, TreeNodeHolderMed hMed, ArrayList<Integer> Weight, int ID, String[] txt, ArrayList<String> Bed) {
        int res;
        final int ColumnTextId = c.getColumnIndex("Text");
        final int ColumnSymptomIDId = c.getColumnIndex("SymptomID");
        final int ColumnShortTextId = c.getColumnIndex("ShortText");
        final int ColumnKoerperTeilId = c.getColumnIndex("KoerperTeilID");
        final int ColumnParentSymptomId = c.getColumnIndex("ParentSymptomID");
        final int ColumnGradeId = c.getColumnIndex("Grade");
        int grade = -1;
        if (ColumnGradeId >= 0) {
            grade = c.getInt(ColumnGradeId);
        }
        int SympID = c.getInt(ColumnSymptomIDId);
        String Text = c.getString(ColumnTextId);
        String ShortText = c.getString(ColumnShortTextId);
        Integer KoerperTeilId = c.getInt(ColumnKoerperTeilId);
        Integer ParentSymptomId = c.getInt(ColumnParentSymptomId);
        boolean found = false;
        String txtCompare = ShortText.toLowerCase();
        if (txt != null && txt.length > 0) {
            for (String t : txt) {
                String regExTXT = null;
                t = Pattern.quote(t.toLowerCase());
                if (_main.blnSearchWholeWord) {
                    regExTXT = "\\b" + t + "\\b";
                } else {
                    regExTXT = ".*" + t + ".*";
                }
                if (txtCompare.matches(regExTXT)) {
                    found = true;
                    break;
                }
            }
        } else {
            found = true;
        }
        if (Bed != null && Bed.size() > 0) {
            for (String t : Bed) {
                String regExBED = null;
                t = Pattern.quote(t.toLowerCase());
                if (_main.blnSearchWholeWord) {
                    regExBED = "\\b" + t + "\\b";
                } else {
                    regExBED = ".*" + t + ".*";
                }
                if (txtCompare.matches(regExBED)) {
                    found |= true;
                    break;
                }
            }
        }

        if (!found) return -1;
        res = -1;
        if (Weight != null && Weight.size() > 0) {
            for (int i = 0; i < Weight.size(); i += 3) {
                int MedID = Weight.get(0 + i);
                int SympID2 = Weight.get(1 + i);
                int Weight2 = Weight.get(2 + i);
                if (MedID >= 0) {
                    if (MedID == hMed.ID && SympID == SympID2) {
                        res = Weight2;
                        break;
                    }
                } else if (SympID == SympID2) {
                    res = Weight2;
                    break;
                }
            }
        } else {
            res = -2;
        }
        String ShortTextHolder = ShortText + (grade >= 0 && res > 0 ? "[" + grade * res + "]" : "(" + grade + ")");
        TreeNode treeNode2 = new TreeNode(new TreeNodeHolderSympt(hMed.getContext(), 1, ShortTextHolder, "Sympt" + SympID, SympID, Text, ShortText, KoerperTeilId, ParentSymptomId, hMed.ID, grade));
        if (res >= 0) treeNode2.setWeight(res);
        try {
            SymptomsActivity.AddNodesRecursive(hMed.getContext(), 0, treeNode2, treeNode, ParentSymptomId, res, hMed.ID);
            return (res == -2 ? 1 : res);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return -1;
        }
        //treeNode2.setLevel(1);
        //treeNode.addChild(treeNode2);

    }

    //private String lastQuery = "";
    public String[] getQueryMed(boolean OrFlag, boolean Wide, boolean blnAdd, ArrayList<Integer> selected) {
        if (!blnAdd) {
            _main.lastQueryMedsMain = "";
            selected.clear();
        }
        String qry = "";
        String qrySymptMed = _main.lastQueryMedsMain;
        List<TreeNode> arr = treeViewMeds.getSelectedNodes();
        int count = arr.size();
        boolean blnMeds = false;
        if (selected.size() > 0 && selected.get(0) > -1) blnMeds = true;
        if (blnMeds) {
            ArrayList<Integer> selNeu = new ArrayList<>();
            for (int i = 0; i < selected.size(); i += 3) {
                int found = selNeu.indexOf(selected.get(i + 1));
                if (found >= 0 && (found - 1) % 3 != 0) {
                    found = -1;
                    for (int ii = 0; ii < selNeu.size(); ii = ii + 3) {
                        if (selNeu.get(ii + 1).equals(selected.get(i + 1))) {
                            found = ii + 1;
                            break;
                        }
                    }
                }
                ;
                if (found >= 0) continue;
                selNeu.add(-1);
                selNeu.add(selected.get(i + 1));
                selNeu.add(selected.get(i + 2));
                blnMeds = false;
            }
            if (!blnMeds) selected = selNeu;
        }
        for (TreeNode t : arr) {
            if (t.getValue() instanceof TreeNodeHolderMed) continue;
            TreeNodeHolderSympt h = (TreeNodeHolderSympt) t.getValue();
            int parentMed = h.ParentMedID;
            int sympID = h.ID;
            int weight = t.getWeight();
            if (!blnMeds) {
                int found = selected.indexOf(new Integer(h.ID));
                if (found >= 0 && (found - 1) % 3 != 0) {
                    found = -1;
                    for (int i = 0; i < selected.size(); i = i + 3) {
                        if (selected.get(i + 1) == h.ID) {
                            found = i + 1;
                            break;
                        }
                    }
                }
                ;
                if (found >= 0) continue;
                selected.add(-1);
                selected.add(sympID);
                selected.add(weight);
            } else {
                selected.add(parentMed);
                selected.add(sympID);
                selected.add(weight);
            }
            if (!lib.libString.IsNullOrEmpty(qrySymptMed)) {
                if (OrFlag)
                    qrySymptMed += " OR ";
                else
                    qrySymptMed += " AND ";
            }
            if (!lib.libString.IsNullOrEmpty(qry)) {
                if (OrFlag)
                    qry += " OR ";
                else
                    qry += " AND ";
            }

            if (!Wide) {
                qry +=
                        "Medikamente.ID in (Select MedikamentID from SymptomeOfMedikament where SymptomID = " + h.ID + ")";
                qrySymptMed += "SymptomeOfMedikament.SymptomID = " + h.ID;
            } else {
                qry +=
                        "Medikamente.ID in (Select MedikamentID from SymptomeOfMedikament where SymptomID IN (SELECT ID FROM Symptome WHERE Text LIKE '%" + MakeFitForQuery(h.SymptomText, true) + "%'))";
                qrySymptMed += "SymptomeOfMedikament.SymptomID IN (SELECT ID FROM Symptome WHERE Text LIKE '%" + MakeFitForQuery(h.SymptomText, true) + "%')";
            }
        }
        if (qrySymptMed != null && qrySymptMed.length() > 0) {
            _main.lastQueryMedsMain = qrySymptMed;
        } else {
            qrySymptMed = _main.lastQueryMedsMain;
        }
        return new String[]{qry, qrySymptMed};

    }


    private void setLightStatusBar(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = view.getSystemUiVisibility();
            _main.getWindow().setStatusBarColor(Color.WHITE);
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
        }
    }

    private void initView(View view) {
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        viewGroup = (RelativeLayout) view.findViewById(R.id.container);
        //_main.setSupportActionBar(toolbar);
        //setLightStatusBar(viewGroup);
    }


}

