package jmg.de.org.repetit;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jmg.de.org.repetit.lib.HackyViewPager;
import jmg.de.org.repetit.lib.dbSqlite;
import jmg.de.org.repetit.lib.lib;
import me.texy.treeview.TreeNode;
import me.texy.treeview.TreeView;

public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";
    private TextView mTextMessage;
    private boolean isTV;
    private boolean isWatch;
    private ViewGroup Layout;
    public HackyViewPager mPager;
    public MyFragmentPagerAdapter fPA;
    public TreeView treeView;
    public dbSqlite db;
    public String lastQuery = "";
    public boolean blnSearchWholeWord;
    public ArrayList<Integer> selected = new ArrayList<>();

    public MainActivity()
    {
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (db == null)
        {
            try
            {
                db = new dbSqlite(this, false);
                db.createDataBase();
            }
            catch (Throwable throwable)
            {
                throwable.printStackTrace();
            }

        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (db != null)
        {
            db.close();
            db = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("lastquery", lastQuery);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (db == null)
        {
            try
            {
                db = new dbSqlite(this, false);
                if (!db.createDataBase()){
                    lib.getDialogOK(this, getString(R.string.DatabaseErrorOlderVersions), getString(R.string.database), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            finish();
                        }
                    });
                }
            }
            catch (Throwable throwable)
            {
                lib.ShowException(this,throwable);
            }

        }
        //lib.main = this;
        lib.gStatus = "MainActivity onCreate";
        //getting the kind of userinterface: television or watch or else
        int UIMode = lib.getUIMode(this);
        switch (UIMode)
        {
            case Configuration.UI_MODE_TYPE_TELEVISION:
                isTV = true;
                break;
            case Configuration.UI_MODE_TYPE_WATCH:
                isWatch = true;
                break;
        }


        setContentView(R.layout.activity_main_viewpager);

        /* Getting a reference to ViewPager from the layout */
        View pager = this.findViewById(R.id.pager);
        Layout = (ViewGroup) pager;
        mPager = (HackyViewPager) pager;
        /* Getting a reference to FragmentManager */
        FragmentManager fm = getSupportFragmentManager();

        setPageChangedListener();

        /* Creating an instance of FragmentPagerAdapter */
        if (fPA == null)
        {
            fPA = new MyFragmentPagerAdapter(fm, this, savedInstanceState != null);
        }

        /* Setting the FragmentPagerAdapter object to the viewPager object */
        mPager.setAdapter(fPA);

        if (savedInstanceState != null)
        {
            lastQuery = savedInstanceState.getString("lastquery");

        } else
        {
            try
            {
                AcceptLicense();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (Throwable throwable)
            {
                throwable.printStackTrace();
            }
        }


    }

    private void AcceptLicense() throws Throwable
    {
        boolean blnLicenseAccepted = getPreferences(Context.MODE_PRIVATE).getBoolean("LicenseAccepted", false);
        if (!blnLicenseAccepted)
        {
            InputStream is = this.getAssets().open("LICENSE");
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String strLicense = s.hasNext() ? s.next() : "";
            s.close();
            is.close();
            lib.yesnoundefined res = (lib.ShowMessageYesNo(this,
                    strLicense,
                    getString(R.string.licenseaccept),
                    true));

            lib.yesnoundefined res2 = lib.AcceptPrivacyPolicy(this, Locale.getDefault());

            if (res == lib.yesnoundefined.yes && res2 == lib.yesnoundefined.yes)
            {
                getPreferences(Context.MODE_PRIVATE).edit().putBoolean("LicenseAccepted", true).commit();
            }
            else
            {
                finish();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        try
        {
            switch (item.getItemId())
            {
                case R.id.mnuCredits:
                    InputStream is = this.getAssets().open("CREDITS");
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    String strCredits = s.hasNext() ? s.next() : "";
                    s.close();
                    is.close();
                    String versionName = this.getPackageManager()
                                             .getPackageInfo(this.getPackageName(), 0).versionName;
                    Spannable spn = lib.getSpanableString(strCredits + "\nV" + versionName);
                    lib.ShowMessage(this, spn, "Credits");
                    break;
                case R.id.mnuPrivacyPolicy:
                    lib.yesnoundefined res2 = lib.AcceptPrivacyPolicy(this, Locale.getDefault());

                    if (res2 == lib.yesnoundefined.yes)
                    {
                        getPreferences(Context.MODE_PRIVATE).edit().putBoolean("PPAccepted", true).commit();
                    } else
                    {
                        getPreferences(Context.MODE_PRIVATE).edit().putBoolean("PPAccepted", false).commit();
                        finish();
                    }
                    break;
                case R.id.mnuContact:
                    Intent intent = new Intent(Intent.ACTION_SEND, Uri.fromParts("mailto", "jhmgbl2@t-online.de", null));
                    intent.setType("message/rfc822");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"jhmgbl2@t-online.de"});
                    String versionName2 = this.getPackageManager()
                                              .getPackageInfo(this.getPackageName(), 0).versionName;
                    intent.putExtra(Intent.EXTRA_SUBJECT, "repetit " + versionName2);
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.Contact));
                    this.startActivity(Intent.createChooser(intent, getString(R.string.SendMail)));
                    break;
                case R.id.mnuHelp:
                    lib.ShowHelp(this,Locale.getDefault());
                    break;
                case R.id.deselect_all:
                    treeView.deselectAll();
                    break;
                case R.id.collapse_all:
                    treeView.collapseAll();
                    break;
                case R.id.show_select_node:
                    Toast.makeText(getApplication(), getSelectedNodes(), Toast.LENGTH_LONG).show();
                    break;
                case R.id.mnuSearchWholeWord:
                    item.setChecked(item.isChecked() ^ true);
                    blnSearchWholeWord = item.isChecked();
                    break;
                case R.id.mnuShowMed:
                    mPager.setCurrentItem(MedActivity.fragID);
                    break;
                case R.id.mnuShowSympt:
                    mPager.setCurrentItem(SymptomsActivity.fragID);
                    break;
                case R.id.mnuFindMeds:
                case R.id.mnuFindMedsAdd:
                    String[] qry;
                    boolean blnAdd = false;
                    if (item.getItemId() == R.id.mnuFindMedsAdd) blnAdd = true;
                    if (mPager.getCurrentItem() == SymptomsActivity.fragID)
                    {
                        qry = fPA.fragSymptoms.getQueryMed(true, false, blnAdd, selected);
                    } else
                        if (mPager.getCurrentItem() == MedActivity.fragID)
                        {
                            qry = fPA.fragMed.getQueryMed(true, false, blnAdd, selected);
                        } else
                        {
                            break;
                        }
                    if (lib.libString.IsNullOrEmpty(qry[1])) break;
                    mPager.setCurrentItem(MedActivity.fragID);
                    //String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                    //        "WHERE " + qry[0] + " AND Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + qry[1] + ")";
                    String qryMedGrade = "Select Medikamente.*, SymptomeOFMedikament.GRADE, SymptomeOFMedikament.SymptomID, Symptome.Text, Symptome.ShortText, Symptome.KoerperTeilID, Symptome.ParentSymptomID FROM SymptomeOfMedikament, Medikamente, Symptome " +
                            "WHERE Medikamente.ID = SymptomeOfMedikament.MedikamentID AND SymptomeOfMedikament.SymptomID = Symptome.ID AND (" + qry[1] + ")";
                    qryMedGrade += " ORDER BY Medikamente.Name, SymptomeOfMedikament.GRADE DESC";
                    fPA.fragMed.buildTreeRep(qryMedGrade, true, null, selected, null);
                    //((MainActivity)getActivity()).fPA.fragMed.buildTree("SELECT * FROM Medikamente WHERE " + qry, true);
                    break;
            }
        }
        catch (Throwable ex)
        {
            Log.e(TAG, "OptionsItemSelected", ex);
        }
        return super.onOptionsItemSelected(item);
    }

    private void setPageChangedListener()
    {
        ViewPager.SimpleOnPageChangeListener pageChangeListener = new ViewPager.SimpleOnPageChangeListener()
        {
            int LastPosition = -1;

            @Override
            public void onPageSelected(int position)
            {
                super.onPageSelected(position);

                if (LastPosition == MedActivity.fragID)
                {
                    try
                    {
                        if (fPA != null && fPA.fragSettings != null)
                        {
                            try
                            {
                                //fPA.fragSettings.saveResultsAndFinish(true);
                            }
                            catch (Throwable ex)
                            {
                                Log.e(".saveResultsAndFinish", ex.getMessage(), ex);
                            }
                                        /*
                                        if (lib.NookSimpleTouch())
                    					{
                    						RemoveFragSettings();
                    					}
                    					*/
                        }

                    }
                    catch (Throwable e)
                    {

                        lib.ShowException(MainActivity.this, e);
                    }
                    //mnuUploadToQuizlet.setEnabled(true);
                } else
                    if (LastPosition == SymptomsActivity.fragID)
                    {
                        if (fPA != null && fPA.fragMed != null)
                        {
                            //fPA.fragMed.removeCallbacks();
                        }
                    }

                if (position == MedActivity.fragID)
                {
                    //mnuAddNew.setEnabled(true);
                    //mnuUploadToQuizlet.setEnabled(true);

                    if (fPA != null && fPA.fragMed != null)
                    {
                        treeView = fPA.fragMed.treeView;
                    /*
                        fPA.fragMed._txtMeaning1.setOnFocusChangeListener(new View.OnFocusChangeListener()
                        {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus)
                            {
                                fPA.fragMed._txtMeaning1.setOnFocusChangeListener(fPA.fragMed.FocusListenerMeaning1);
                                if (hasFocus)
                                {
                                    fPA.fragMed._scrollView.fullScroll(View.FOCUS_UP);
                                }

                            }
                        });
                        */
                    }
                } else
                    if (position == SettingsActivity.fragID)
                    {
                        if (fPA != null && fPA.fragSettings != null)
                        {
                            try
                            {
                            /*
                            int Language = fPA.fragSettings.getIntent().getIntExtra(
                                    "Language", org.de.jmg.learn.vok.Vokabel.EnumSprachen.undefiniert.ordinal());
                            fPA.fragSettings.spnLanguages.setSelection(Language);
                            fPA.fragSettings.setSpnMeaningPosition();
                            fPA.fragSettings.setSpnWordPosition();
                            fPA.fragSettings.setChkTSS();
                            */
                            }
                            catch (Throwable ex)
                            {
                                Log.e(".saveResultsAndFinish", ex.getMessage(), ex);
                            }
                        }
                    } else
                        if (position == SymptomsActivity.fragID)
                        {
                            if (fPA != null && fPA.fragSymptoms != null)
                            {
                                treeView = fPA.fragSymptoms.treeView;
                                //searchQuizlet();
                            }

                        } else
                        {
                            //mnuAddNew.setEnabled(false);
                        }

                LastPosition = position;


            }

        };

        mPager.addOnPageChangeListener(pageChangeListener);

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


}
