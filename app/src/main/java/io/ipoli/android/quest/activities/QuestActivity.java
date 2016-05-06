package io.ipoli.android.quest.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ipoli.android.Constants;
import io.ipoli.android.R;
import io.ipoli.android.app.BaseActivity;
import io.ipoli.android.app.events.EventSource;
import io.ipoli.android.app.events.ScreenShownEvent;
import io.ipoli.android.quest.QuestContext;
import io.ipoli.android.quest.QuestNotificationScheduler;
import io.ipoli.android.quest.commands.StartQuestCommand;
import io.ipoli.android.quest.commands.StopQuestCommand;
import io.ipoli.android.quest.data.Quest;
import io.ipoli.android.quest.events.CompleteQuestRequestEvent;
import io.ipoli.android.quest.events.DoneQuestTapEvent;
import io.ipoli.android.quest.events.EditQuestRequestEvent;
import io.ipoli.android.quest.events.StartQuestTapEvent;
import io.ipoli.android.quest.events.StopQuestTapEvent;
import io.ipoli.android.quest.persistence.QuestPersistenceService;
import io.ipoli.android.quest.ui.formatters.TimerFormatter;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 2/1/16.
 */
public class QuestActivity extends BaseActivity implements Chronometer.OnChronometerTickListener {

    public static final String ACTION_QUEST_CANCELED = "io.ipoli.android.intent.action.QUEST_CANCELED";
    public static final String ACTION_START_QUEST = "io.ipoli.android.intent.action.START_QUEST";

    @Bind(R.id.root_container)
    CoordinatorLayout rootContainer;

    @Bind(R.id.quest_details_progress)
    ProgressBar timerProgress;

    @Bind(R.id.quest_details_timer)
    FloatingActionButton timerButton;

    @Bind(R.id.quest_details_time)
    Chronometer timer;

    @Bind(R.id.quest_details_name)
    TextView name;

    @Bind(R.id.quest_details_edit)
    ImageButton edit;

    @Inject
    QuestPersistenceService questPersistenceService;

    @Inject
    Bus eventBus;

    private boolean questHasDuration;
    private Quest quest;

    private boolean isTimerRunning;
    private int elapsedSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || TextUtils.isEmpty(getIntent().getStringExtra(Constants.QUEST_ID_EXTRA_KEY))) {
            finish();
            return;
        }
        setContentView(R.layout.activity_quest);
        ButterKnife.bind(this);
        appComponent().inject(this);

        String questId = getIntent().getStringExtra(Constants.QUEST_ID_EXTRA_KEY);
        questPersistenceService.findById(questId).subscribe(q -> {
            quest = q;
            initUI();

            Intent intent = getIntent();
            String action = intent.getAction();

            if (ACTION_QUEST_CANCELED.equals(action)) {
                new StopQuestCommand(quest, questPersistenceService, this).execute();
            } else if (ACTION_START_QUEST.equals(action)) {
                new StartQuestCommand(this, questPersistenceService, quest).execute();
            }
        });

        eventBus.post(new ScreenShownEvent("quest"));
    }

    private void initUI() {
        setBackgroundColors(Quest.getContext(quest));
        questHasDuration = quest.getDuration() > 0;
        resetTimerUI();
        elapsedSeconds = 0;
        name.setText(quest.getName());
    }

    private void setBackgroundColors(QuestContext ctx) {
        rootContainer.setBackgroundColor(ContextCompat.getColor(this, ctx.resDarkerColor));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, ctx.resDarkerColor));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, ctx.resDarkerColor));
    }

    private void resetTimerUI() {
        timer.setBase(0);
        int minuteDuration = questHasDuration ? quest.getDuration() : 0;
        timer.setText(TimerFormatter.format(TimeUnit.MINUTES.toMillis(minuteDuration)));
        timerProgress.setProgress(0);
        long totalTime = questHasDuration ?
                TimeUnit.MINUTES.toMillis(quest.getDuration()) :
                TimeUnit.MINUTES.toMillis(Constants.QUEST_WITH_NO_DURATION_TIMER_MINUTES);

        timerProgress.setMax((int) TimeUnit.MILLISECONDS.toSeconds(totalTime));
        timerProgress.setSecondaryProgress((int) TimeUnit.MILLISECONDS.toSeconds(totalTime));
    }

    @Override
    protected void onResume() {
        QuestNotificationScheduler.stopTimer(quest.getId(), this);
        if (Quest.isStarted(quest)) {
            elapsedSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - quest.getActualStart().getTime());
            resumeTimer();
            timerButton.setImageResource(R.drawable.ic_stop_white_32dp);
            edit.setVisibility(View.GONE);
        }

        if(quest.isHabit()) {
            edit.setVisibility(View.GONE);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isTimerRunning) {
            return;
        }
        stopTimer();
        long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - quest.getActualStart().getTime());
        boolean isOverdue = questHasDuration && quest.getDuration() - elapsedMinutes < 0;
        if (isOverdue) {
            return;
        }
        QuestNotificationScheduler.scheduleUpdateTimer(quest.getId(), this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == Constants.EDIT_QUEST_RESULT_REQUEST_CODE) {
            questPersistenceService.findById(quest.getId()).subscribe(q -> {
                quest = q;
                initUI();
            });
        } else if (resultCode == Constants.RESULT_REMOVED) {
            finish();
        }
    }

    private void startTimer() {
        elapsedSeconds = 0;
        resumeTimer();
    }

    private void resumeTimer() {
        timer.setOnChronometerTickListener(this);
        timer.start();
        isTimerRunning = true;
    }

    private void stopTimer() {
        timer.setOnChronometerTickListener(null);
        timer.stop();
        isTimerRunning = false;
    }

    @OnClick(R.id.quest_details_timer)
    public void onTimerTap(View v) {
        if (isTimerRunning) {
            eventBus.post(new StopQuestTapEvent(quest));
            stopTimer();
            new StopQuestCommand(quest, questPersistenceService, this).execute();
            resetTimerUI();
            timerButton.setImageResource(R.drawable.ic_play_arrow_white_32dp);
            edit.setVisibility(View.VISIBLE);
        } else {
            eventBus.post(new StartQuestTapEvent(quest));
            new StartQuestCommand(this, questPersistenceService, quest).execute();
            startTimer();
            timerButton.setImageResource(R.drawable.ic_stop_white_32dp);
            edit.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.quest_details_done)
    public void onDoneTap(View v) {
        eventBus.post(new DoneQuestTapEvent(quest));
        stopTimer();
        eventBus.post(new CompleteQuestRequestEvent(quest, EventSource.QUEST));
        finish();
    }

    @OnClick(R.id.quest_details_edit)
    public void onEditTap(View v) {
        eventBus.post(new EditQuestRequestEvent(quest, EventSource.QUEST));
        Intent i = new Intent(this, EditQuestActivity.class);
        i.putExtra(Constants.QUEST_ID_EXTRA_KEY, quest.getId());
        startActivityForResult(i, Constants.EDIT_QUEST_RESULT_REQUEST_CODE);
    }

    @Override
    public void onChronometerTick(Chronometer chronometer) {
        long nowMillis = quest.getActualStart().getTime() + TimeUnit.SECONDS.toMillis(elapsedSeconds);
        long questDurationSeconds = TimeUnit.MINUTES.toSeconds(quest.getDuration());

        timerProgress.setProgress((int) getTimerProgress(elapsedSeconds));

        if (questHasDuration && isOverdue(questDurationSeconds)) {
            showOverdueTime(questDurationSeconds);
        } else if (questHasDuration) {
            showCountDownTime(nowMillis);
        } else {
            showCountUpTime(nowMillis);
        }

        elapsedSeconds++;
    }

    private void showOverdueTime(long questDurationSeconds) {
        long overdueMillis = TimeUnit.SECONDS.toMillis(elapsedSeconds - questDurationSeconds);
        timer.setText("+" + TimerFormatter.format(overdueMillis));
    }

    private void showCountDownTime(long nowMillis) {
        long endTimeMillis = quest.getActualStart().getTime() + TimeUnit.MINUTES.toMillis(quest.getDuration());
        timer.setText(TimerFormatter.format(endTimeMillis - nowMillis));
    }

    private void showCountUpTime(long nowMillis) {
        long timerMillis = nowMillis - quest.getActualStart().getTime();
        timer.setText(TimerFormatter.format(timerMillis));
    }


    private boolean isOverdue(long questDurationSeconds) {
        return questDurationSeconds < elapsedSeconds;
    }

    private long getTimerProgress(long elapsedSeconds) {
        if (questHasDuration) {
            // the progress is set to max if elapsed seconds is larger than max progress
            return elapsedSeconds;
        } else {
            long defaultDurationSeconds = TimeUnit.MINUTES.toSeconds(Constants.QUEST_WITH_NO_DURATION_TIMER_MINUTES);
            return elapsedSeconds % defaultDurationSeconds;
        }
    }
}
