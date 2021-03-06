package com.example.khong.term4_java1d;

import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.database.*;

public class MachineCellHolder extends RecyclerView.ViewHolder {

    private CountDownTimer machineCountdownTimer;
    private TextView machineName;
    private TextView machineTimeData;
    private TextView machineTimeLabel;
    private ImageView machineStatus;
    private ImageButton btnMachineNotify;
    private boolean notifyState;
    private FirebaseController firebaseController;
    private DatabaseReference userDatabase;
    private String userTopicChoice;
    private DatabaseReference userTopicChoiceRef;
    private boolean btnNotifyBool;

    private boolean collected;
    private String machineTopic;
    private long storedSecondsElapsed;

    MachineCellHolder(final View cellView) {
        super(cellView);

        Log.d("MachineCellHolder", "Created");

        firebaseController = FirebaseController.getInstance();

        machineName = cellView.findViewById(R.id.machine_name);
        machineTimeData = cellView.findViewById(R.id.machine_timedata);
        machineTimeLabel = cellView.findViewById(R.id.machine_timelabel);
        machineStatus = cellView.findViewById(R.id.machine_icon);
        btnMachineNotify = cellView.findViewById(R.id.machine_notifButton);

        userDatabase = firebaseController.getUserDatabase();

        this.btnMachineNotify.setEnabled(false);
    }

    public void setMachineTopic(final String machineTopic) {
        Log.d("MachineCellHolder", "Set Topic");
        this.machineTopic = machineTopic;

        Log.d("MachineCellHolder", machineTopic);

        final long timeElapsed = storedSecondsElapsed;

        userTopicChoiceRef = userDatabase.child("subscriptions").child(machineTopic);
        ValueEventListener topicChoiceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {
                    userTopicChoice = dataSnapshot.getValue().toString();
                    notifyState = userTopicChoice.equals("true");
                    Log.d("MachineCellHolder", userTopicChoice);
                } catch (NullPointerException e) {
                    Log.e("MachineCellHolder", "Null preference, set to false");
                    userTopicChoiceRef.setValue("false");
                    notifyState = false;
                } finally {
                    if (notifyState) {
                        btnNotifyBool = true;
                        turnOnNotifications();
                    } else {
                        btnNotifyBool = false;
                        turnOffNotifications();
                    }

                    setMachineTimeData(timeElapsed);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        userTopicChoiceRef.addListenerForSingleValueEvent(topicChoiceListener);


        View.OnClickListener machineNotifyOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnNotifyBool) {
                    turnOffNotifications();
                    btnNotifyBool = false;
                } else {
                    btnNotifyBool = true;
                    turnOnNotifications();
                }
            }
        };

        Log.e("MachineCellHolder", "Set btnMachineNotify listener");
        btnMachineNotify.setOnClickListener(machineNotifyOnClickListener);


    }

    public void setMachineTimeData(long secondsElapsed) {
        storedSecondsElapsed = secondsElapsed;

        Log.d("MachineCellHolder", "Set Time Data");

        try {
            machineCountdownTimer.cancel();
        } catch (Exception e) {
            Log.d("MachineCellHolder", "Timer does not exist");
        } finally {
            machineCountdownTimer = null;
        }

        final long startMillis = secondsElapsed * 1000;

        if (secondsElapsed < (45 * 60)) {
            long timeToComplete = (45 * 60) - secondsElapsed;
            setMachineTimeLabel("since cycle start");
            setMachineStatus("RED");
            machineCountdownTimer = new CountDownTimer(timeToComplete * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    machineTimeData.setText(formatTimeData(millisUntilFinished));
                }

                @Override
                public void onFinish() {
                    setMachineTimeLabel("wash cycle finished");
                    if (collected) {
                        setMachineStatus("GREEN");
                    } else {
                        setMachineStatus("YELLOW");
                    }
                }
            }.start();
        } else {
            setMachineTimeLabel("since cycle finished");
            setMachineStatus("YELLOW");
            if (collected) {
                // confirm collected
                setMachineStatus("GREEN");
            } else if (secondsElapsed > (2 * 60 * 60)) {
                // 2 hours later, probably collected
                setMachineStatus("GREEN");
            }
            machineCountdownTimer = new CountDownTimer(startMillis - (45 * 60 * 1000), 1000) {
                long counter = 0;
                long timeData = 0;

                @Override
                public void onTick(long millisUntilFinished) {
                    counter = counter + 2000;
                    timeData = millisUntilFinished + counter;
                    machineTimeData.setText(formatTimeData(timeData));
                }

                @Override
                public void onFinish() {
                    // it's a count up timer, and should never actually finish!
                }
            }.start();
        }
    }

    public void setMachineStatus(String machineStatus) {
        if (machineStatus.equals("GREEN")) {
            Log.d("MachineCellHolder", "Set Status Green");
            this.machineStatus.setImageResource(R.drawable.ic_assets_greencircle);
            this.btnMachineNotify.setEnabled(false);
            notifyState = false;
            turnOffNotifications();
        } else if (machineStatus.equals("YELLOW")) {
            Log.d("MachineCellHolder", "Set Status Yellow");
            this.machineStatus.setImageResource(R.drawable.ic_assets_yellowcircle);
            this.btnMachineNotify.setEnabled(true);
        } else {
            Log.d("MachineCellHolder", "Set Status Red");
            this.machineStatus.setImageResource(R.drawable.ic_assets_redcircle);
            this.btnMachineNotify.setEnabled(true);
        }
    }

    private void turnOnNotifications() {
        Log.e(machineTopic, "Notifications On");
        btnMachineNotify.setImageResource(R.drawable.ic_assets_darkbluebell);
        userTopicChoiceRef.setValue("true");
        firebaseController.subscribeTopic(machineTopic);
    }

    private void turnOffNotifications() {
        Log.e(machineTopic, "Notifications Off");
        btnMachineNotify.setImageResource(R.drawable.ic_assets_lightbluebell);
        userTopicChoiceRef.setValue("false");
        firebaseController.unsubscribeTopic(machineTopic);
    }

    public void setMachineTimeDataOnly(long secondsElapsed) {
        storedSecondsElapsed = secondsElapsed;
    }

    public void setMachineName(String machineName) {
        this.machineName.setText(machineName);
    }

    public void setMachineTimeLabel(String machineTimeLabel) {
        this.machineTimeLabel.setText(machineTimeLabel);
    }

    private String formatTimeData(long timeData) {
        long secondData = timeData / 1000;
        int mh_raw = (int) (secondData) / 60;
        int hours = mh_raw / 60;
        int minutes = mh_raw % 60;
        int seconds = (int) (secondData) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getMachineTopic() {
        return machineTopic;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        Log.d("MachineCellHolder", "Set Collected");
        this.collected = collected;

        if (collected) {
            setMachineStatus("GREEN");
            btnMachineNotify.setEnabled(false);
        } else {
            btnMachineNotify.setEnabled(true);
        }

    }


}
