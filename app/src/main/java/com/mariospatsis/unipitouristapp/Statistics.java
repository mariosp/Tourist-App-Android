package com.mariospatsis.unipitouristapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class Statistics extends AppCompatActivity {

    String FCMToken = "";
    int nearPoisToday=0;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;
    ArrayList<nearPois> nearPois;
    TextView nearPoisTodayLabel;
    TextView sliked;
    TextView sviewed;
    String viewsFir = "0";
    String likesFir = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        nearPoisTodayLabel = (TextView) findViewById(R.id.s_nearPoisToday);
        sliked = (TextView) findViewById(R.id.s_liked);
        sviewed = (TextView) findViewById(R.id.s_viewed);
        FCMToken = intent.getExtras().getString("FCMToken");
        myRef= database.getReference().child("users").child(FCMToken);//Επιλεγουμε μονο τον χρηστη με αυτο το τοκεν
        getFirebaseRef();
    }

    void getFirebaseRef (){
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                nearPois = new ArrayList<>(); //Αρχικοποιηση λιστας
                nearPois np = new nearPois();
                //αποθηκευουμε ολα τα αντικειμενα της βασης nearPOIS του συγκεκριμενου χρηστη σε μια λιστα
                //ετσι ωστε να γινει επεξεργασια

                viewsFir = dataSnapshot.child("views").getValue(String.class);
                likesFir = dataSnapshot.child("likes").getValue(String.class);

                sliked.setText(getString(R.string.m_liked,likesFir));
                sviewed.setText(getString(R.string.m_viewed,viewsFir));

                for(DataSnapshot ds : dataSnapshot.child("nearPois").getChildren()) {
                    np = new nearPois();

                    np = ds.getValue(nearPois.class);
                    nearPois.add(np);
                }

                getNearPoisStats();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value

            }
        });

    }

    private void getNearPoisStats(){
        String previousId ="";

        for(nearPois np : nearPois) {
            if(DateUtils.isToday(Long.parseLong(np.getTimestamp())*1000)){
                nearPoisToday++;
            }
        }

        nearPoisTodayLabel.setText(getString(R.string.m_nearpoistoday, nearPoisToday));


    }


    @Override
    public boolean onSupportNavigateUp() {
        Intent gotoMainActivity = new Intent(this, MainActivity.class);
        startActivity(gotoMainActivity);
        this.finish();
        return true;
    }
}
