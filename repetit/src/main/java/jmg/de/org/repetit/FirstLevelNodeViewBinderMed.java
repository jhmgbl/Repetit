package jmg.de.org.repetit;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.AppCompatSpinner;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.AppCompatImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import jmg.de.org.repetit.lib.dbSqlite;
import jmg.de.org.repetit.lib.lib;
import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeView;
import me.texy.treeview.base.CheckableNodeViewBinder;
import me.texy.treeview.base.SpinnerNodeViewBinder;

/**
 * Created by zxy on 17/4/23.
 */

public class FirstLevelNodeViewBinderMed extends SpinnerNodeViewBinder implements View.OnLongClickListener {
    TextView textView;
    AppCompatImageView imageView;
    private TreeNode treeNode;

    public FirstLevelNodeViewBinderMed(View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(R.id.node_name_view);
        imageView = (AppCompatImageView) itemView.findViewById(R.id.arrow_img);
        //((MainActivity)(treeView.context)).registerForContextMenu(itemView);
        //itemView.setOnLongClickListener(this);
        itemView.setLongClickable(true);
    }

    @Override
    public int getSpinnerViewId() {
        return -1;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_first_level_med;
    }

    @Override
    public void bindView(final TreeNode treeNode) {
        textView.setText(treeNode.getValue().toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            imageView.setRotation(treeNode.isExpanded() ? 90 : 0);
        }
        this.treeNode = treeNode;
        treeNode.holder = this;
        if (lib.NookSimpleTouch()) {
            itemView.setBackgroundColor(Color.WHITE);
            textView.setTextColor(Color.BLACK);
            imageView.setBackgroundColor(Color.BLACK);
        }
        /*textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                FirstLevelNodeViewBinderMed.this.treeView.collapseNode(treeNode);
                treeNode.getChildren().clear();
                onNodeToggled(treeNode,true);
                return false;
            }
        });*/
    }

    @Override
    public void onNodeToggled(TreeNode treeNode, boolean expand) {
        onNodeToggled(treeNode,expand,null);
    }


    @Override
    public void onNodeToggled(TreeNode treeNode, boolean expand, ArrayList<TreeNode>children) {

        if (expand) {
            try {
                buildTree(treeView,treeNode,children);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                imageView.animate().rotation(90).setDuration(200);
            }

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                imageView.animate().rotation(0).setDuration(200);
            }
        }
    }
    public static void buildTree(TreeView tv, TreeNode treeNodeParent, ArrayList<TreeNode> children) throws  Throwable {
        if (treeNodeParent.getChildren().size()>0) return;
        TreeNodeHolder h = (TreeNodeHolder) treeNodeParent.getValue();
        int ParentMedID = -1;
        dbSqlite db = h.getContext().db;
        try {
            Cursor c;
            if (h.getClass() == MedActivity.TreeNodeHolderMed.class) {
                ParentMedID = ((MedActivity.TreeNodeHolderMed)h).ID;
                c = db.query("Select Symptome.*, SymptomeOfMedikament.Grade FROM SymptomeOfMedikament, Symptome WHERE SymptomeOfMedikament.MedikamentID = " + ParentMedID +
                        " AND Symptome.ParentSymptomID IS Null AND Symptome.ID = SymptomeOfMedikament.SymptomID ORDER BY Symptome.Text COLLATE NOCASE");
            }
            else
            {
                throw new RuntimeException("not a med!"); //c = db.query("Select Symptome.* FROM Symptome WHERE Symptome.ParentSymptomID = " + ((TreeNodeHolderSympt)h).ID + " ORDER BY Symptome.Text");
            }
                try {
                if (c.moveToFirst()) {
                    int ColumnTextId = c.getColumnIndex("Text");
                    int ColumnIDId = c.getColumnIndex("ID");
                    int ColumnShortTextId = c.getColumnIndex("ShortText");
                    int ColumnKoerperTeilId = c.getColumnIndex("KoerperTeilID");
                    int ColumnParentSymptomId = c.getColumnIndex("ParentSymptomID");
                    int ColumeGradeID = c.getColumnIndex("Grade");
                    do {
                        int ID = c.getInt(ColumnIDId);
                        String Text = c.getString(ColumnTextId);
                        boolean found = false;
                        if (children != null)
                        {
                            for (TreeNode T: children)
                            {
                                if (((TreeNodeHolderSympt) T.getValue()).SymptomText.equalsIgnoreCase(Text))
                                {
                                    treeNodeParent.addChild(T);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (found) continue;
                        String ShortText = c.getString(ColumnShortTextId);
                        Integer KoerperTeilId = c.getInt(ColumnKoerperTeilId);
                        Integer ParentSymptomId = c.getInt(ColumnParentSymptomId);
                        Integer Grade = c.getInt(ColumeGradeID);
                        TreeNode treeNode = new TreeNode(new TreeNodeHolderSympt(h.getContext(), 1, ShortText + "(" + Grade + ")", "Sympt" + ID, ID, Text, ShortText, KoerperTeilId, ParentSymptomId, ParentMedID,Grade));
                        treeNode.setLevel(1);
                        treeNodeParent.addChild(treeNode);
                    } while (c.moveToNext());
                    tv.expandNode(treeNodeParent);
                }
            } finally {
                c.close();
            }
        }
        finally
        {
            db.close();
        }
    }


    @Override
    public boolean onLongClick(View v) {
        FirstLevelNodeViewBinderMed.this.treeView.collapseNode(treeNode);
        treeNode.getChildren().clear();
        onNodeToggled(treeNode,true);
        return false;}
}
