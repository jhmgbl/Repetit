package jmg.de.org.repetit;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jmg.de.org.repetit.lib.dbSqlite;
import jmg.de.org.repetit.lib.lib;
import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeView;

import static jmg.de.org.repetit.lib.lib.libString.MakeFitForQuery;


public class MedActivity extends Fragment
{
    public final static int fragID = 0;
    private static final String TAG = "MedActivity";

    public MainActivity _main;

    protected Toolbar toolbar;
    private ViewGroup viewGroup;
    private TreeNode root;
    public TreeView treeView;
    private AppCompatEditText txtSearch;
    private ImageButton btnSearchAnd;
    private ImageButton btnSearchOr;
    private String _lastQuery;
    private String[] _txt;
    private ArrayList<Integer> Selected;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        _main = ((MainActivity) getActivity());
    }

    @Override
    public void onViewCreated(View view, Bundle savedinstancestate)
    {

        //initTreeView(view);
    }

    public void initTreeView(View view)
    {
        initView(view);

        root = TreeNode.root();
        if (lib.libString.IsNullOrEmpty(_lastQuery) && !lib.libString.IsNullOrEmpty(_main.lastQuery)) {
            String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                    "WHERE Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + _main.lastQuery + ")";
            qryMedGrade += " ORDER BY Medikamente.Name, SymptomeOfMedikament.GRADE DESC";
            buildTreeRep(qryMedGrade, false, null, Selected);
        }
        else if (! lib.libString.IsNullOrEmpty(_lastQuery)) {
            buildTreeRep(_lastQuery, false, _txt, Selected);
        }
        else {
            buildTree("SELECT * FROM Medikamente ORDER BY Name", false);
        }
        treeView = new TreeView(root, _main, new MyNodeViewFactoryMed());
        View view2 = treeView.getView();
        view2.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        viewGroup.addView(view2);
        if (_main.treeView == null) _main.treeView = treeView;


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedinstancestate)
    {
        View v = null;
        try
        {
            v = inflater.inflate(R.layout.activity_med, container, false);
            txtSearch = (AppCompatEditText)v.findViewById(R.id.txtSearch);
            txtSearch.setImeOptions(EditorInfo.IME_ACTION_DONE);
            txtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    String txt = txtSearch.getText().toString();
                    if (!lib.libString.IsNullOrEmpty(txt)) searchSymptoms(txt, true);
                    return true;
                }
            });
            btnSearchAnd = (ImageButton) v.findViewById(R.id.btnSearchAnd);
            btnSearchAnd.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    String txt = txtSearch.getText().toString();
                    if (!lib.libString.IsNullOrEmpty(txt)) searchSymptoms(txt,true);
                }
            });
            btnSearchOr = (ImageButton) v.findViewById(R.id.btnSearchOr);
            btnSearchOr.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    String txt = txtSearch.getText().toString();
                    if (!lib.libString.IsNullOrEmpty(txt)) searchSymptoms(txt,false);
                }
            });
            if (savedinstancestate!=null) {
                _lastQuery = savedinstancestate.getString("lastquery");
                _txt = savedinstancestate.getStringArray("txt");
                Selected = savedinstancestate.getIntegerArrayList("Selected");
            }

            initTreeView(v);
            restoreTreeView(savedinstancestate);
            return v;
        }
        catch (Throwable ex)
        {
            lib.ShowException(_main,ex);
            return v;
        }
    }

    private void restoreTreeView(Bundle savedinstancestate) throws Throwable
    {
        if (treeView != null && savedinstancestate != null)
        {
            if (root != null)
            {
                ArrayList<Integer> expMed = savedinstancestate.getIntegerArrayList("expMed");
                ArrayList<Integer> expMedSymp = savedinstancestate.getIntegerArrayList("expMedSymp");
                if (expMed.size() == 0 ) return;
                for (TreeNode t : root.getChildren())
                {
                    if (t.hasChild() == false || true)
                    {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) t.getValue();
                        if (expMed.contains(h.ID))
                        {
                            expMed.remove(new Integer(h.ID));
                            while(expMedSymp.size()>0 && expMedSymp.get(0)==-99) expMedSymp.remove(0);
                            if (t.hasChild() == false) FirstLevelNodeViewBinderMed.buildTree(treeView,t);
                            else treeView.expandNode(t);
                            lib.gStatus = "expSympMed";
                            expSympMed(h.ID,t,expMedSymp,Selected);
                        }
                        if (expMed.size() == 0) break;

                    }
                }
                if (Selected.size()>0) {
                    for (TreeNode t : root.getChildren())
                    {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) t.getValue();
                        while (Selected.size()>0 && h.ID == Selected.get(0)) {
                            Selected.remove(0);
                            SymptomsActivity.AddNodesRecursive(_main, 1, null, t, Selected.get(0),Selected.get(1), h.ID);
                            Selected.remove(0);
                            Selected.remove(0);
                        }
                    }
                }

            }

        }
    }

    private void expSympMed(int id, TreeNode t, ArrayList<Integer> expMedSymp, ArrayList<Integer> selected) throws Throwable
    {

        CheckSelected (id,t,selected);
        if (expMedSymp.size()==0) return;
        if (-99 == expMedSymp.get(0) && -99 == expMedSymp.get(1))
        {
            expMedSymp.remove(0);
            expMedSymp.remove(0);
            return;
        }


        for (TreeNode tt : t.getChildren())
        {
            if (expMedSymp.size()==0) return;

            if (-99 == expMedSymp.get(0) && -99 == expMedSymp.get(1))
            {
                expMedSymp.remove(0);
                expMedSymp.remove(0);
                break;
            }
            TreeNodeHolderSympt h = (TreeNodeHolderSympt) tt.getValue();
            if (h.ParentMedID == expMedSymp.get(0) && h.ID == expMedSymp.get(1))
            {
                expMedSymp.remove(0);
                expMedSymp.remove(0);
                if(tt.hasChild()==false)SecondLevelNodeViewBinder.buildTree(treeView,tt);
                else treeView.expandNode(tt);
                if (expMedSymp.size()<=0)
                {
                    CheckSelected (id,tt,selected);
                    break;
                }
                if (-99 == expMedSymp.get(0) && -99 == expMedSymp.get(1))
                {
                    expMedSymp.remove(0);
                    expMedSymp.remove(0);
                    CheckSelected (id,tt,selected);
                }
                else
                {
                    expSympMed(id,tt,expMedSymp,selected);
                }

            }
        }
    }

    private void CheckSelected(int id,TreeNode t, ArrayList<Integer> selected)
    {
        for (TreeNode tt : t.getChildren())
        {
            if (selected.size()<=0) break;
            TreeNodeHolderSympt h = (TreeNodeHolderSympt) tt.getValue();
            for (int i = 0; i < selected.size(); i += 3)
            {
                if (selected.get(i) == id && selected.get(i + 1) == h.ID)
                {
                    treeView.selectNode(tt,1);
                    tt.setWeight(selected.get(i+2));
                    selected.remove(i);
                    selected.remove(i);
                    selected.remove(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("lastquery",_lastQuery);
        outState.putStringArray("txt", _txt);
        if (treeView != null)
        {
            if (root != null)
            {
                ArrayList<Integer> expMed = new ArrayList<>();
                ArrayList<Integer> expMedSymp = new ArrayList<>();
                ArrayList<Integer> Selected = new ArrayList<>();
                for (TreeNode t : root.getChildren())
                {
                    if (t.hasChild() && t.isExpanded())
                    {
                        TreeNodeHolderMed h = (TreeNodeHolderMed) t.getValue();
                        expMed.add(h.ID);
                        getSympMed(h.ID,t,expMedSymp);
                    }
                }
                for (TreeNode t : treeView.getSelectedNodes())
                {
                    if (t.getValue() instanceof  TreeNodeHolderSympt)
                    {
                        TreeNodeHolderSympt h = (TreeNodeHolderSympt) t.getValue();
                        Selected.add(h.ParentMedID);
                        Selected.add(h.ID);
                        Selected.add(t.getWeight());
                        //Selected.add(root.getWeight());
                    }
                }
                outState.putIntegerArrayList("expMed", expMed);
                outState.putIntegerArrayList("expMedSymp", expMedSymp);
                outState.putIntegerArrayList("Selected",Selected);
            }

        }
    }

    private void getSympMed(int Medid, TreeNode t, ArrayList<Integer> sympMed)
    {
        boolean hasChild = false;
        for (TreeNode tt: t.getChildren())
        {
            if (tt.hasChild() && tt.isExpanded())
            {
                TreeNodeHolderSympt h = (TreeNodeHolderSympt) tt.getValue();
                sympMed.add(Medid);
                sympMed.add(h.ID);
                getSympMed(Medid,tt,sympMed);
                hasChild = true;
            }
            else
            {

            }
        }
        if (hasChild)
        {
            sympMed.add (-99);
            sympMed.add (-99);
        }

    }

    public static String getWhereWhole(String column, String search)
    {
        return "WHERE " + column + " like '% " + search + " %' OR " + column + " like '" + search + " %' OR " + column + " like '% " + search + "' OR " + column + " like '" + search + "'";
    }

    private void searchSymptoms(String searchtxt, boolean AndFlag) {
        if (lib.libString.IsNullOrEmpty(searchtxt)) return;
        String[] txt = searchtxt.split("\\.");
        try {
            //String qry = "SELECT Medikamente.* FROM Symptome WHERE ";
            if (!lib.libString.IsNullOrEmpty(_main.lastQuery))
            {
                lib.yesnoundefined res =(lib.ShowMessageYesNo(getContext(),getString(R.string.alreadysearched),getString(R.string.continuesearch),false));
                if (res != lib.yesnoundefined.yes) return;
            }
            String where = "";
            for (String s : txt) {
                String whereWhole = null;
                if (!lib.libString.IsNullOrEmpty(s)) {
                    s = MakeFitForQuery(s,true);
                    if (AndFlag) {
                        if (!(where.equalsIgnoreCase(""))) where += " AND ";
                        if (txt.length > 1)
                            where += "SymptomeOfMedikament.SymptomID IN (SELECT ID FROM Symptome " + (_main.blnSearchWholeWord?getWhereWhole("Text",s):"WHERE Text LIKE '%" +s + "%'") + ")";
                        else
                            where += "SymptomeOfMedikament.SymptomID IN (SELECT ID FROM Symptome " + (_main.blnSearchWholeWord?getWhereWhole("ShortText",s):"WHERE ShortText LIKE '%" + s + "%'") + ")";
                    }
                    else
                    {
                        if (!(where.equalsIgnoreCase(""))) where += " OR ";
                        where += "SymptomeOfMedikament.SymptomID IN (SELECT ID FROM Symptome " + (_main.blnSearchWholeWord?getWhereWhole("ShortText",s):"WHERE ShortText LIKE '%" + MakeFitForQuery(s, true) + "%'") + ")";
                    }
                }
            }
            if(!AndFlag||txt.length<2) txt=null;
            //AddSymptomeQueryRecursive(root,qry,-1,true);
            String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                    "WHERE Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + where + ")";
            qryMedGrade += " ORDER BY Medikamente.Name, SymptomeOfMedikament.GRADE DESC";
            buildTreeRep(qryMedGrade,true,txt,null);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
    public String getSelectedNodes()
    {
        StringBuilder stringBuilder = new StringBuilder("You have selected: ");
        List<TreeNode> selectedNodes = treeView.getSelectedNodes();
        for (int i = 0; i < selectedNodes.size(); i++)
        {
            if (i < 5)
            {
                stringBuilder.append(selectedNodes.get(i).getValue().toString()).append(",");
            } else
            {
                stringBuilder.append("...and ").append(selectedNodes.size() - 5).append(" more.");
                break;
            }
        }
        return stringBuilder.toString();
    }


    public class TreeNodeHolderMed extends TreeNodeHolder
    {
        public final int ID;
        public final String Name;
        public final String Beschreibung;
        public int totalGrade;
        public int count;

        public TreeNodeHolderMed(MainActivity context, int level, String Text, String path, int ID, String Name, String Beschreibung)
        {
            super(level, Text, path, context);
            this.ID = ID;
            this.Name = Name;
            this.Beschreibung = Beschreibung;
        }
    }


    public void buildTree(String qry, boolean refresh)
    {
        if (root.getChildren().size() > 0)
        {
            List<TreeNode> l = root.getChildren();
            l.clear();
            root.setChildren(l);
        }
        dbSqlite db = ((MainActivity)getActivity()).db;
        try
        {

            Cursor c = db.query(qry);
            try
            {
                if (c.moveToFirst())
                {
                    int ColumnNameId = c.getColumnIndex("Name");
                    int ColumnIDId = c.getColumnIndex("ID");
                    int ColumnBeschreibungId = c.getColumnIndex("Beschreibung");
                    do
                    {
                        int ID = c.getInt(ColumnIDId);
                        String Name = c.getString(ColumnNameId);
                        String Beschreibung = c.getString(ColumnBeschreibungId);
                        TreeNode treeNode = new TreeNode(new TreeNodeHolderMed((MainActivity)getActivity(),0, Name, "Med" + ID, ID, Name, Beschreibung));
                        treeNode.setLevel(0);
                        root.addChild(treeNode);

                    } while (c.moveToNext());
                }
            }
            finally
            {
                c.close();
            }

        }
        finally
        {
            db.close();
        }

        if (refresh && treeView != null) treeView.refreshTreeView();

    }
    public void buildTreeRep(String qry, boolean refresh, String[] txt, ArrayList<Integer>selected)
    {
        boolean Initialized = true;
        final String CodeLoc = TAG + ".initTreeview";
        lib.gStatus = CodeLoc + " Start";
        int MedID;
        //ArrayList<TreeNodeHolderMed> arrMed = new ArrayList<>();
        if (root.getChildren().size() > 0)
        {
            List<TreeNode> l = root.getChildren();
            l.clear();
            root.setChildren(l);
        }
        dbSqlite db = ((MainActivity)getActivity()).db;
        try
        {
            Cursor c = db.query(qry);
            try
            {
                if (c.moveToFirst())
                {
                    int ColumnNameId = c.getColumnIndex("Name");
                    int ColumnIDId = c.getColumnIndex("ID");
                    int ColumnBeschreibungId = c.getColumnIndex("Beschreibung");
                    int ColumnGrade = c.getColumnIndex("Grade");

                    do
                    {
                        //if(!c.moveToNext()) break;
                        int ID = c.getInt(ColumnIDId);
                        String Name = c.getString(ColumnNameId);
                        String Beschreibung = c.getString(ColumnBeschreibungId);
                        int sum = 0;
                        int nexts = 0;
                        TreeNodeHolderMed hMed = new TreeNodeHolderMed((MainActivity)getActivity(),0, Name, "Med" + ID, ID, Name, Beschreibung);
                        TreeNode treeNode = new TreeNode(hMed);
                        treeNode.setLevel(0);
                        root.addChild(treeNode);
                        int f = insertSymptom(c,treeNode,hMed,selected,ID,txt);
                        if (f == -2) f = 1;
                        if (f >= 0){
                            sum = c.getInt(ColumnGrade) * f;
                            nexts += 1;
                        }
                        while (c.moveToNext() && c.getInt(ColumnIDId) == ID) {
                            f = insertSymptom(c,treeNode,hMed,selected,ID,txt);
                            if (f == -2) f = 1;
                            if(f>=0) {
                                nexts += 1;
                                sum += c.getInt(ColumnGrade) * f;
                            }
                        }
                        hMed.totalGrade = sum;
                        hMed.count = nexts;
                        hMed.Text += "(" + hMed.totalGrade + "/" + hMed.count + ")";
                    } while (!c.isAfterLast());
                    List<TreeNode> l = root.getChildren();

                    Collections.sort(l, new Comparator<TreeNode>()
                    {
                        @Override
                        public int compare(TreeNode lhs, TreeNode rhs)
                        {
                            TreeNodeHolderMed h1 = (TreeNodeHolderMed) lhs.getValue();
                            TreeNodeHolderMed h2 = (TreeNodeHolderMed) rhs.getValue();
                            if (h1.totalGrade>h2.totalGrade) return -1;
                            if (h1.totalGrade==h2.totalGrade && h1.count>h2.count) return -1;
                            if (h1.totalGrade==h2.totalGrade && h1.count == h2.count) return 0;
                            return 1;
                        }
                    });
                    root.setChildren(l);
                    //if (refresh) treeView.refreshTreeView();

                }
            }
            finally
            {
                c.close();
            }

        }
        finally
        {
            db.close();
        }

        if (refresh && treeView != null) treeView.refreshTreeView();
        _lastQuery = qry;
        _txt = txt;

    }

    private int insertSymptom(Cursor c, TreeNode treeNode, TreeNodeHolderMed hMed, ArrayList<Integer> Weight, int ID, String[] txt)
    {
        int res;
        final int ColumnTextId = c.getColumnIndex("Text");
        final int ColumnSymptomIDId = c.getColumnIndex("SymptomID");
        final int ColumnShortTextId = c.getColumnIndex("ShortText");
        final int ColumnKoerperTeilId = c.getColumnIndex("KoerperTeilID");
        final int ColumnParentSymptomId = c.getColumnIndex("ParentSymptomID");
        final int ColumnGradeId = c.getColumnIndex("Grade");
        int grade = -1;
        if (ColumnGradeId >= 0)
        {
            grade = c.getInt(ColumnGradeId);
        }
        int SympID = c.getInt(ColumnSymptomIDId);
        String Text = c.getString(ColumnTextId);
        String ShortText = c.getString(ColumnShortTextId);
        Integer KoerperTeilId = c.getInt(ColumnKoerperTeilId);
        Integer ParentSymptomId = c.getInt(ColumnParentSymptomId);
        boolean found = false;
        if (txt!=null && txt.length>0)
        {
            for (String t : txt)
            {
                if (ShortText.toLowerCase().contains(t.toLowerCase()))
                {
                    found = true;
                    break;
                }
            }
        }
        else
        {
            found = true;
        }
        if (!found) return -1;
        res = -1;
        if (Weight!=null) {
            for (int i = 0; i < Weight.size(); i += 3) {
                int MedID = Weight.get(0+i);
                int SympID2 = Weight.get(1+i);
                int Weight2 = Weight.get(2+i);
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
        }
        else
        {
            res = -2;
        }
        ShortText+= (grade >=0 && res > 0 ? "("+ grade * res + ")":"");
        TreeNode treeNode2 = new TreeNode(new TreeNodeHolderSympt(hMed.getContext(), 1, ShortText, "Sympt" + SympID, SympID, Text, ShortText, KoerperTeilId, ParentSymptomId,hMed.ID,grade));
        if (res >= 0 ) treeNode2.setWeight(res);
        try {
            SymptomsActivity.AddNodesRecursive(hMed.getContext(),0,treeNode2,treeNode,ParentSymptomId, res,hMed.ID);
            return  (res == -2?1:res);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return  -1;
        }
        //treeNode2.setLevel(1);
        //treeNode.addChild(treeNode2);

    }
    //private String lastQuery = "";
    public String[] getQueryMed(boolean OrFlag, boolean Wide, boolean blnAdd, ArrayList<Integer> selected) {
        if (!blnAdd)
        {
            _main.lastQuery = "";
            selected.clear();
        }
        String qry = "";
        String qrySymptMed = _main.lastQuery;
        List<TreeNode> arr = treeView.getSelectedNodes();
        int count = arr.size();
        for (TreeNode t : arr) {
            if (t.getValue() instanceof  TreeNodeHolderMed) continue;
            TreeNodeHolderSympt h = (TreeNodeHolderSympt) t.getValue();
            int found = selected.indexOf(new Integer(h.ID));
            if(found >=0 && (found-1)%3!=0) {
                found = -1;
                for (int i = 0; i < selected.size(); i = i + 3)
                {
                    if (selected.get(i+1) == h.ID)
                    {
                        found = i+1;
                        break;
                    }
                }
            };
            if (found >= 0) continue;
            selected.add(-1); selected.add(h.ID); selected.add(t.getWeight());
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
        _main.lastQuery = qrySymptMed;
        return new String[]{qry, qrySymptMed};

    }


    private void setLightStatusBar (@NonNull View view){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int flags = view.getSystemUiVisibility();
            _main.getWindow().setStatusBarColor(Color.WHITE);
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
        }
    }

    private void initView(View view)
    {
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        viewGroup = (RelativeLayout) view.findViewById(R.id.container);
        //_main.setSupportActionBar(toolbar);
        //setLightStatusBar(viewGroup);
    }
}

