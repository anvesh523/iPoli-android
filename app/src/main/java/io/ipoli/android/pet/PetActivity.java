package io.ipoli.android.pet;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ipoli.android.Constants;
import io.ipoli.android.R;
import io.ipoli.android.app.App;
import io.ipoli.android.app.activities.BaseActivity;
import io.ipoli.android.app.events.EventSource;
import io.ipoli.android.app.events.ScreenShownEvent;
import io.ipoli.android.app.help.HelpDialog;
import io.ipoli.android.app.persistence.OnDataChangedListener;
import io.ipoli.android.app.ui.dialogs.TextPickerFragment;
import io.ipoli.android.app.utils.ResourceUtils;
import io.ipoli.android.app.utils.StringUtils;
import io.ipoli.android.pet.data.Pet;
import io.ipoli.android.pet.events.PetRenamedEvent;
import io.ipoli.android.pet.events.RevivePetRequest;
import io.ipoli.android.player.Player;
import io.ipoli.android.player.persistence.PlayerPersistenceService;
import io.ipoli.android.store.StoreItemType;
import io.ipoli.android.store.activities.StoreActivity;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 8/23/16.
 */
public class PetActivity extends BaseActivity implements OnDataChangedListener<Player>, TextPickerFragment.OnTextPickedListener {

    @Inject
    PlayerPersistenceService playerPersistenceService;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.root_container)
    View backgroundImage;

    @BindView(R.id.pet_picture)
    ImageView picture;

    @BindView(R.id.pet_picture_state)
    ImageView pictureState;

    @BindView(R.id.pet_xp_bonus)
    TextView xpBonus;

    @BindView(R.id.pet_coins_bonus)
    TextView coinsBonus;

    @BindView(R.id.pet_state)
    TextView state;

    @BindView(R.id.pet_hp)
    ProgressBar hp;

    @BindView(R.id.revive)
    FancyButton revive;

    private Player player;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet);
        App.getAppComponent(this).inject(this);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        eventBus.post(new ScreenShownEvent(this, EventSource.PET));
    }

    @Override
    protected void onStart() {
        super.onStart();
        playerPersistenceService.listen(this);
    }

    @Override
    protected void onStop() {
        playerPersistenceService.removeAllListeners();
        super.onStop();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_pick_daily_challenge_quests).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pet_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_rename_pet:
                showRenamePetDialog();
                return true;
            case R.id.action_store:
                Intent intent = new Intent(this, StoreActivity.class);
                intent.putExtra(StoreActivity.START_ITEM_TYPE, StoreItemType.PETS.name());
                startActivity(intent);
                return true;
            case R.id.action_help:
                HelpDialog.newInstance(R.layout.fragment_help_dialog_pet, R.string.help_dialog_pet_title, "pet").show(getSupportFragmentManager());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRenamePetDialog() {
        TextPickerFragment.newInstance(player.getPet().getName(), R.string.rename_your_pet, this).show(getSupportFragmentManager());
    }

    @Override
    public void onTextPicked(String name) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        renamePet(name);
    }

    private void renamePet(String name) {
        player.getPet().setName(name);
        playerPersistenceService.save(player);
        eventBus.post(new PetRenamedEvent(name));
    }

    @Override
    public void onDataChanged(Player player) {
        this.player = player;
        Pet pet = player.getPet();
        getSupportActionBar().setTitle(pet.getName());
        picture.setImageResource(pet.getCurrentAvatar().picture);
        String statePicture = getResources().getResourceEntryName(pet.getCurrentAvatar().picture) + "_" + pet.getStateText();
        pictureState.setImageDrawable(getDrawable(ResourceUtils.extractDrawableResource(this, statePicture)));
        xpBonus.setText(String.format(getString(R.string.pet_xp), pet.getExperienceBonusPercentage()));
        coinsBonus.setText(String.format(getString(R.string.pet_coins), pet.getCoinsBonusPercentage()));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) revive.getIconImageObject().getLayoutParams();
        params.gravity = Gravity.CENTER_VERTICAL;

        if (pet.getState() == Pet.PetState.DEAD) {
            revive.setText(Constants.REVIVE_PET_COST + "");
            revive.setVisibility(View.VISIBLE);
            hp.setVisibility(View.GONE);
            state.setVisibility(View.GONE);
        } else {
            revive.setVisibility(View.GONE);
            hp.setVisibility(View.VISIBLE);
            state.setVisibility(View.VISIBLE);
            state.setText(getString(Pet.PetState.getNameRes(pet.getState())).toUpperCase());
            hp.setProgress(pet.getHealthPointsPercentage());
        }
    }

    @OnClick(R.id.revive)
    public void onReviveClick(View view) {
        Pet pet = player.getPet();
        eventBus.post(new RevivePetRequest(pet.getCurrentAvatar()));
        long playerCoins = player.getCoins();
        if (playerCoins < Constants.REVIVE_PET_COST) {
            String message = String.format(getString(R.string.pet_revive_not_enough_coins), pet.getName());
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }
        player.removeCoins(Constants.REVIVE_PET_COST);
        pet.addHealthPoints(Constants.DEFAULT_PET_HP);
        playerPersistenceService.save(player);
    }
}