package com.tw.iotdemo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement;
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory;
import com.estimote.proximity_sdk.api.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.api.ProximityObserver;
import com.estimote.proximity_sdk.api.ProximityObserverBuilder;
import com.estimote.proximity_sdk.api.ProximityZone;
import com.estimote.proximity_sdk.api.ProximityZoneBuilder;
import com.estimote.proximity_sdk.api.ProximityZoneContext;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {
    private ProximityObserver proximityObserver;
    private GoogleSignInClient mGoogleSignInClient;
    private Integer RC_SIGN_IN = 1;
    private GoogleSignInAccount loggedInAccount;
    private String CHANNEL_ID = "default";
    private static final int NOTIFICATION_ID = 1;
    private List<String> availableRooms = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createSignInActivity();

        setContentView(R.layout.activity_main);

        EstimoteCloudCredentials cloudCredentials =
                new EstimoteCloudCredentials("iotdemo-ldm", "283794a072f659625c22af3392b771af");

        this.proximityObserver = createProximityObserver(cloudCredentials);

        final ProximityZone zone = createProximityZone();

        RequirementsWizardFactory
                .createEstimoteRequirementsWizard()
                .fulfillRequirements(this,
                        // onRequirementsFulfilled
                        new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                Log.d("app", "requirements fulfilled");
                                proximityObserver.startObserving(zone);
                                return null;
                            }
                        },
                        // onRequirementsMissing
                        new Function1<List<? extends Requirement>, Unit>() {
                            @Override
                            public Unit invoke(List<? extends Requirement> requirements) {
                                Log.e("app", "requirements missing: " + requirements);
                                return null;
                            }
                        },
                        // onError
                        new Function1<Throwable, Unit>() {
                            @Override
                            public Unit invoke(Throwable throwable) {
                                Log.e("app", "requirements error: " + throwable);
                                return null;
                            }
                        });

        //createNotificationChannel();
        //createNotification();
    }

    private void createNotification() {

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("IOTDemo")
                .setContentText("Do want to do this?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @NotNull
    private ProximityZone createProximityZone() {
        return new ProximityZoneBuilder()
                .forTag("IOT-Community")
                .inNearRange()
                .onEnter(new Function1<ProximityZoneContext, Unit>() {
                    @Override
                    public Unit invoke(ProximityZoneContext context) {
                        String roomName = context.getAttachments().get("room_name");
                        String msg = "Welcome " + loggedInAccount.getGivenName() + " to " + roomName + "'s desk";
                        Log.d("app", msg);
                        addRomm(roomName);
                        updateUI();
//                        Toasty.info(getApplicationContext(), msg, Toast.LENGTH_LONG, true).show();
                        return null;
                    }
                })
                .onExit(new Function1<ProximityZoneContext, Unit>() {
                    @Override
                    public Unit invoke(ProximityZoneContext context) {
                        String roomName = context.getAttachments().get("room_name");
                        String msg = "Bye bye " + loggedInAccount.getGivenName() + ", come again!";
                        Log.d("app", msg);
                        removeRoom(roomName);
                        updateUI();
//                        Toasty.info(getApplicationContext(), msg, Toast.LENGTH_LONG, true).show();
                        return null;
                    }
                })
                .build();
    }

    @NotNull
    private ProximityObserver createProximityObserver(EstimoteCloudCredentials cloudCredentials) {
        return new ProximityObserverBuilder(getApplicationContext(), cloudCredentials)
                .onError(new Function1<Throwable, Unit>() {
                    @Override
                    public Unit invoke(Throwable throwable) {
                        Log.e("app", "proximity observer error: " + throwable);
                        return null;
                    }
                })
                .withBalancedPowerMode()
                .build();
    }

    private void createSignInActivity() {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null)
            signIn();
        else {
            loggedInAccount = account;
        }

    }

    private void addRomm(String room) {
        if (!availableRooms.contains(room)) {
            availableRooms.add(room);
        }
    }

    private void removeRoom(String room) {
        if (availableRooms.contains(room)) {
            availableRooms.remove(room);
        }
    }

    private void updateUI() {
        RadioGroup rooms = findViewById(R.id.rooms);
        TextView txtNoRoomAvailable = findViewById(R.id.txtNoRoomAvailable);
        TextView txtSelectRoom = findViewById(R.id.txtSelectRoom);
        rooms.removeAllViews();
        if (availableRooms.size() == 0) {
            txtNoRoomAvailable.setVisibility(View.VISIBLE);
            txtSelectRoom.setVisibility(View.INVISIBLE);
        } else {
            txtNoRoomAvailable.setVisibility(View.INVISIBLE);
            txtSelectRoom.setVisibility(View.VISIBLE);

            for (String roomName : availableRooms) {
                RadioButton rdbtn = new RadioButton(this);
                rdbtn.setId(View.generateViewId());
                rdbtn.setText(roomName);
                rooms.addView(rdbtn);
            }
        }


        Button buttonBookRoom = findViewById(R.id.buttonBookRoom);
        buttonBookRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (availableRooms.size() > 0) {
                    RadioGroup rooms = findViewById(R.id.rooms);
                    int selectedRoomId = rooms.getCheckedRadioButtonId();
                    if (selectedRoomId != -1) {
                        RadioButton selectedRoom = rooms.findViewById(selectedRoomId);
                        Toasty.success(getApplicationContext(), "Room " + selectedRoom.getText() + " booked successfully.", Toast.LENGTH_LONG, true).show();
                    }
                    else{
                        Toasty.info(getApplicationContext(), "Please select room first.", Toast.LENGTH_LONG, true).show();
                    }
                }
                else{
                    Toasty.error(getApplicationContext(), "No room available near you.", Toast.LENGTH_LONG, true).show();
                }
            }
        });

    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            loggedInAccount = account;
            // Signed in successfully, show authenticated UI.
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("app", "signInResult:failed code=" + e.getStatusCode());
        }
    }


}
