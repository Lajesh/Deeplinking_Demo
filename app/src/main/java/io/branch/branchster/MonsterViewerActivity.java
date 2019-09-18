package io.branch.branchster;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import io.branch.branchster.fragment.InfoFragment;
import io.branch.branchster.util.MonsterImageView;
import io.branch.branchster.util.MonsterPreferences;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CommerceEvent;
import io.branch.referral.util.CurrencyType;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.Product;
import io.branch.referral.util.ProductCategory;
import io.branch.referral.util.ShareSheetStyle;

public class MonsterViewerActivity extends FragmentActivity implements InfoFragment.OnFragmentInteractionListener {
    private static String TAG = MonsterViewerActivity.class.getSimpleName();
    public static final String MY_MONSTER_OBJ_KEY = "my_monster_obj_key";
    MonsterImageView monsterImageView_;
    BranchUniversalObject myMonsterObject_;

    private String carModel;
    private String name;
    private String contactNumber;

    EditText textModel;
    EditText textName;
    EditText textPhone;
    Button btnBook;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monster_viewer);
        initUI();

    }
    
    private void initUI() {
        monsterImageView_ = (MonsterImageView) findViewById(R.id.monster_img_view);
        textModel = (EditText) findViewById(R.id.carModel);
        textName = (EditText) findViewById(R.id.name);
        textPhone = (EditText) findViewById(R.id.contactNo);
        btnBook = (Button) findViewById(R.id.btnBook);
        if (Branch.getInstance().isAutoDeepLinkLaunch(this)) {
            MonsterPreferences pref = MonsterPreferences.getInstance(this);
            myMonsterObject_ = BranchUniversalObject.getReferredBranchUniversalObject();
            textModel.setText(myMonsterObject_.getContentMetadata().getCustomMetadata().get("$car_model"));
            textName.setText(myMonsterObject_.getContentMetadata().getCustomMetadata().get("$name"));
            textPhone.setText(myMonsterObject_.getContentMetadata().getCustomMetadata().get("$contact_number"));
            textModel.setEnabled(false);
            textName.setEnabled(false);
            textPhone.setEnabled(false);
            btnBook.setText("Proceed");
            pref.saveMonster(myMonsterObject_);
        } else {
            myMonsterObject_ = getIntent().getParcelableExtra(MY_MONSTER_OBJ_KEY);
        }
        if (myMonsterObject_ != null) {
            String monsterName = getString(R.string.monster_name);
            if (!TextUtils.isEmpty(myMonsterObject_.getTitle())) {
                monsterName = myMonsterObject_.getTitle();
            } else if (myMonsterObject_.getContentMetadata().getCustomMetadata().containsKey("monster_name")) {
                monsterName = myMonsterObject_.getContentMetadata().getCustomMetadata().get("monster_name");
            }
            String description = MonsterPreferences.getInstance(this).getMonsterDescription();
            if (!TextUtils.isEmpty(myMonsterObject_.getDescription())) {
                description = myMonsterObject_.getDescription();
            }
//            ((TextView) findViewById(R.id.txtDescription)).setText(description);
            // set my monster image
           // monsterImageView_.setMonster(myMonsterObject_);
        } else {
            Log.e(TAG, "Monster is null. Unable to view monster");
        }

        //Share monster
        findViewById(R.id.btnBook).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!Branch.getInstance().isAutoDeepLinkLaunch(MonsterViewerActivity.this)) {
                    shareMyMonster();
                    new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
                            .addCustomDataProperty("MonsterName", myMonsterObject_.getTitle())
                            .setCurrency(CurrencyType.USD)
                            .setDescription(myMonsterObject_.getDescription())
                            .addContentItems(myMonsterObject_)
                            .logEvent(MonsterViewerActivity.this);
                }else{
                    Toast.makeText(MonsterViewerActivity.this, "Feature coming soon", Toast.LENGTH_SHORT).show();
                }
                
            }
        });
    }
    
    /**
     * Method to share my custom monster with sharing with Branch Share sheet
     */
    private void shareMyMonster() {
        LinkProperties linkProperties = new LinkProperties()
                .addTag("myMonsterTag1")
                //.setAlias("myCustomMonsterLink") // In case you need to white label your link
                .setFeature("myMonsterSharefeature1")
                .setStage("1")
                .addControlParameter("$android_deeplink_path", "monster/view/")
                .addControlParameter("$car_model", textModel.getText().toString())
                .addControlParameter("$name", textName.getText().toString())
                .addControlParameter("$contact_number", textPhone.getText().toString());
        final String monsterName = myMonsterObject_.getTitle();
        String shareTitle = "Booking details ";
        String shareMessage = "I just booked Maruti Suzuki " +  textModel.getText().toString() + " .\n\nSee the booking details here:\n";
        String copyUrlMessage = "Save  url";
        String copiedUrlMessage = "Added  url to clipboard";
        ShareSheetStyle shareSheetStyle = new ShareSheetStyle(MonsterViewerActivity.this, shareTitle, shareMessage)
                .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), copyUrlMessage, copiedUrlMessage)
                .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "More options")
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER);
        myMonsterObject_.showShareSheet(MonsterViewerActivity.this, linkProperties, shareSheetStyle, new Branch.BranchLinkShareListener() {
                    Random rnd = new Random(System.currentTimeMillis());
                    Double revenue;
                    
                    @Override
                    public void onShareLinkDialogLaunched() {
                        JSONObject jsonObject = new JSONObject();
                        revenue = rnd.nextInt(4) + 1D;
                        try {
                            jsonObject.put("sku", monsterName);
                            jsonObject.put("price", revenue);
                        } catch (JSONException e) {
                        }
                        Branch.getInstance(getApplicationContext()).userCompletedAction("Add to Cart", jsonObject);
                    }
                    
                    @Override
                    public void onShareLinkDialogDismissed() {
                    }
                    
                    @Override
                    public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
                    }
                    
                    @Override
                    public void onChannelSelected(String channelName) {
                        Product branchster = new Product();
                        branchster.setSku(monsterName);
                        branchster.setPrice(revenue);
                        branchster.setQuantity(1);
                        branchster.setVariant("X-Tra Hairy");
                        branchster.setBrand("Branch");
                        branchster.setCategory(ProductCategory.ANIMALS_AND_PET_SUPPLIES);
                        branchster.setName(monsterName);
                        CommerceEvent commerceEvent = new CommerceEvent();
                        commerceEvent.setRevenue(revenue);
                        commerceEvent.setCurrencyType(CurrencyType.USD);
                        commerceEvent.addProduct(branchster);
                        Branch.getInstance().sendCommerceEvent(commerceEvent, null, null);
                    }
                },
                new Branch.IChannelProperties() {
                    @Override
                    public String getSharingTitleForChannel(String channel) {
                        return channel.contains("Messaging") ? "title for SMS" :
                                channel.contains("Slack") ? "title for slack" :
                                        channel.contains("Gmail") ? "title for gmail" : null;
                    }
                    
                    @Override
                    public String getSharingMessageForChannel(String channel) {
                        return channel.contains("Messaging") ? "message for SMS" :
                                channel.contains("Slack") ? "message for slack" :
                                        channel.contains("Gmail") ? "message for gmail" : null;
                    }
                });
    }
    
    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to exit?")
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).create().show();
        }
    }
    
    @Override
    public void onFragmentInteraction() {
        //no-op
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initUI();
    }
}
