package com.mariospatsis.unipitouristapp;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class PreviewPoi extends AppCompatActivity implements View.OnClickListener {
    pois poi;
    TextView title;
    ImageView mImageView;
    TextView summary;
    TextView category;
    ImageButton speak;
    TextToSpeech speaktext;
    TextView likelabel;
    TextView viewslabel;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference().child("pois");
    Button like;
    String likesFir = "0";
    DatabaseReference usersRef = database.getReference().child("users");
    String FCMToken;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_activity);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        poi = (pois) getIntent().getSerializableExtra("poi");
        Intent intent = getIntent();
        FCMToken = intent.getExtras().getString("FCMToken");

        title = (TextView) findViewById(R.id.p_title);
        summary = (TextView) findViewById(R.id.textView4);
        mImageView = (ImageView) findViewById(R.id.p_img);
        title = (TextView) findViewById(R.id.p_title);
        category = (TextView) findViewById(R.id.p_category);
        speak = (ImageButton) findViewById(R.id.p_speak);
        likelabel = (TextView) findViewById(R.id.p_likelabel);
        viewslabel = (TextView) findViewById(R.id.p_viewslabel);
        like = (Button) findViewById(R.id.p_like);
        like.setOnClickListener(this);
        firUserRef();



        setPoi();
        speak.setOnClickListener(this);
         speaktext =new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                System.out.println("text");
                if(status != TextToSpeech.ERROR) {
                    speaktext.setLanguage(Locale.getDefault());
                }
            }
        });
    }

    private void setPoi(){
        new ImageDownloadAsync(mImageView).execute(poi.getImgUrl());
        title.setText(poi.getTitle());
        summary.setText(poi.getSummary());
        category.setText(getString(R.string.p_category,poi.getCategory()));
        likelabel.setText(getString(R.string.p_likelabel,poi.getLike()));
        viewslabel.setText(getString(R.string.p_viewlabel,poi.getViews()));
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent gotoMainActivity = new Intent(this, MainActivity.class);
        startActivity(gotoMainActivity);
        this.finish();
        return true;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.p_speak:
                speaktext.speak(poi.getTitle()+".", TextToSpeech.QUEUE_ADD, null);
                speaktext.speak(category.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                speaktext.speak(poi.getSummary(), TextToSpeech.QUEUE_ADD, null);
                break;
            case R.id.p_like:
                myRef.child(String.valueOf(poi.getId())).child("like").setValue(poi.getLike() + 1);
                likelabel.setText(getString(R.string.p_likelabel,poi.getLike()+1));
                like.setEnabled(false);
                int newlikesFir = Integer.parseInt(likesFir) +1 ;
                usersRef.child(FCMToken).child("likes").setValue(String.valueOf(newlikesFir));

        }


    }

    private void firUserRef(){
        usersRef.child(FCMToken).child("likes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                likesFir = dataSnapshot.getValue(String.class);

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value

            }
        });
    }
}
