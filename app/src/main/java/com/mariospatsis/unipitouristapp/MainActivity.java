package com.mariospatsis.unipitouristapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,LocationListener{
    LocationManager locationManager;
    final static int REQUESTCODE = 324;
    SeekBar seekbar;
    Button showPoi;
    TextView selectedtext;
    TextView titletext;
    ImageView mImageView;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference().child("pois");
    DatabaseReference usersRef = database.getReference().child("users");
    int selectedm = 400;
    pois nearPoi;
    int nearDistance;
    ArrayList<pois> poislist = new ArrayList<>();
    String FCMToken;
    TextView distanceLabel;
    String viewsFir = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //O LOCATION_SERVICE για προσβαση στο service της τοποθεσιας κινητου GPS
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        selectedtext =(TextView) findViewById(R.id.main_textselected);
        titletext = (TextView) findViewById(R.id.main_texttitle);
        seekbar.setProgress(5); //Δηλωση της default τιμης που θα εχει η μπαρα (Δηλαδη το 5 ειναι το 400)
        selectedtext.setText(getString(R.string.main_textselected, selectedm));
        mImageView = (ImageView) findViewById(R.id.imageView);
        showPoi = (Button) findViewById(R.id.main_showpoi);
        distanceLabel = (TextView) findViewById(R.id.main_distance);
        showPoi.setVisibility(View.GONE); //κατα την αρχικοποιηση δεν εμφανιζουμε το κουμπι show poi
        showPoi.setOnClickListener(this);
        barListener();
        checkPermision();
        getFirebaseRef();

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( this,  new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                FCMToken = instanceIdResult.getToken();
                System.out.println(FCMToken);
                firUserRef();

                usersRef.child(FCMToken).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null && dataSnapshot.getChildren() != null &&
                                dataSnapshot.getChildren().iterator().hasNext()) {
                            //FCMTOKEN exists
                        } else {
                            //FCMTOKEN does not exists
                            Map<String, String> data = new HashMap<String, String>();
                            data.put("nearPois", "");
                            data.put("likes", "0");
                            data.put("views", "0");
                            usersRef.child(FCMToken).setValue(data);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });
    }


    //H setOnSeekBarChangeListener ακουει στις αλλαγες του αντικειμενου SeekBar
    //Οταν ο χρηστης αλλαξει την μπαρα καλειται η onProgressChanged οπου καθοριζει τα μετρα μου θα επιλεξει ο χρηστης
    void barListener(){
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                switch(progress){
                    case 0:
                        selectedm = 10;
                        break;
                    case 1:
                        selectedm = 50;
                        break;
                    case 2:
                        selectedm = 100;
                        break;
                    case 3:
                        selectedm = 200;
                        break;
                    case 4:
                        selectedm = 300;
                        break;
                    case 5:
                        selectedm = 400;
                        break;
                    case 6:
                        selectedm = 500;
                        break;
                    case 7:
                        selectedm = 600;
                        break;
                    case 8:
                        selectedm = 700;
                        break;
                    case 9:
                        selectedm = 1000;
                        break;
                    case 10:
                        selectedm = 1200;
                        break;
                    default:
                        selectedm = 400;
                        break;
                }

                //Εμφανιζομαι τα μετρα που εχει επιλεξει ο χρηστης με το αντικειμενο seekBar
                String set = getString(R.string.main_textselected, selectedm);
                selectedtext.setText(set);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    //Χρησιμοποιουμε τον valueEventListener της firebase για να ακουσουμε στις αλλαγες τις βασης
    //Οταν καλουμε για διαβασμα ή οταν οι τιμες στην βαση αλλαξουν καλειται η onDataChange()
    void getFirebaseRef (){
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                 poislist = new ArrayList<>(); //Αρχικοποιηση λιστας
                pois poi = new pois();

                //Διαβασμα τον δεδομενων απο την βαση
                //Σε καθε επαναληψη δημιουργειται ενα αντικειμενο της class pois
                //οπου με την getValue(CLASSNAME) μπορουμε να κανουμε map
                //τα δεδομενα της βασης με αυτα της κλασης αρκει να υπαρχουν στην κλαση τα ιδια ονοματα μεταβλητων
                //με αυτα της βασης. Τελος καθε αντικειμενο αποθηκευεται στην λιστα
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    poi = new pois();

                     poi = ds.getValue(pois.class);
                     poi.setId(Integer.parseInt(ds.getKey()));
                     poislist.add(poi);

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.option_menu_en:
                setLang("en");
                return true;
            case R.id.option_menu_el:
                setLang("el");
                return true;
            case R.id.m_statistics:
                Intent goToStatistics = new Intent(this,Statistics.class);
               goToStatistics.putExtra("FCMToken", FCMToken);
                startActivity(goToStatistics);
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setLang(String locale){
        Locale mylocale = new Locale(locale);
        Locale.setDefault(mylocale);
        Configuration config = new Configuration();
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        recreate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(this,"Yesss I have GPS",Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            System.out.println("test");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                    (LocationListener) this);
        } else
            Toast.makeText(this,"I need this permission!...",Toast.LENGTH_SHORT).show();
    }

//H onLocationChanged καλειται καθε φορα που υπαρχει αλλαγη της τοποθεσιας GPS
    @Override
    public void onLocationChanged(Location location) {
        int minMeters= -1; // Αρχικοποιηση μεταβλητης
        pois selectedPoi = null; // Αρχικοποιηση επιλεγμενου poi σε null
        double latitude = 0;
        double longtitude = 0;

        if(location==null){
                //Σε περιπτωση που δεν υπαρχει στιγμα απο το GPS
        }else{
            //Σε περιπτωση που υπαρχει σημα
            //Ελεγχουμε αμα η λιστα poislist δεν ειναι αδεια. Αυτο γιατι μπορει
            //και να μην εχει γινει το γεμισμα της λιστας ακομη απο την βαση
            if(!poislist.isEmpty()) {
                for (pois poi : poislist) {
                    //συντεταγμενες που υπαρχουν για το συγκεκριμενο poi της λιστας
                    //ετσι ωστε να δημιουργηθει ενα αντικειμενο location με αυτες
                    //με σκοπο την χρηση του στην μεθοδο distanceTo που υπολογιζει την αποσταση σε μετρα
                    // των δυο συντεταγμενων (poi - θεση χρηστη)
                     latitude = poi.getLatitude();
                     longtitude = poi.getLongtitude();

                    Location poiLocation = new Location("poilocation");
                    poiLocation.setLatitude(latitude);
                    poiLocation.setLongitude(longtitude);

                    float distance = location.distanceTo(poiLocation);
                    int resultDis = Math.round(distance); // αποσταση σε μετρα

                    //Βρισκουμε το poi με την μικροτερη αποσταση αμα βρισκονται ενα η παραπανω μεσα στην περιοχη που εχει οριστει
                    if ((minMeters == -1 && resultDis <= selectedm) || (resultDis <= selectedm && minMeters > resultDis)) {
                        minMeters = resultDis;
                        selectedPoi = poi;
                    }

                }

                //Αμα υπαρχει poi που ειναι κοντα ο χρηστης τοτε θα εχει αλλαξει η μεταβλητη minMeters και θα μπει στο if
                if (minMeters >= 0) {
                    distanceLabel.setText(getString(R.string.main_distance,nearDistance));
                    if(!selectedPoi.equals(nearPoi)) { //ελεγχος για το αν ειναι διαφορετικο το poi απο το προηγουμενο ετσι ωστε να μην γινεται η αλλαγη αμα δεν βρεθει κοντα σε αλλο
                        nearPoi = selectedPoi; //το τελικο αντικειμενο της κλασης pois
                        nearDistance = minMeters; //η αποσταση σε μετρα του poi απο το σημειο του χρηστη
                        setPoiView(latitude,longtitude); //φτιαχνουμε το UI να εμφανιζει στο πανω μερος τον τιτλο την εικονα και το κουμπι του poi
                    }
                }else{
                    //Διαφορετικα αμα δεν βρισκεται κοντα σε poi
                    //Αρχικοποιει στις default times που ειχε
                    erasePoi();
                }
            }

        }
    }

    private void setPoiView(double latitude,double longitutde){
        titletext.setText(nearPoi.getTitle());
        new ImageDownloadAsync(mImageView).execute(nearPoi.getImgUrl());
        showPoi.setVisibility(View.VISIBLE);

        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        Map<String, String> data = new HashMap<String, String>();
        data.put("id", String.valueOf(nearPoi.getId()));
        data.put("latitude", Double.toString(latitude));
        data.put("longitude",Double.toString(longitutde));
        data.put("timestamp",ts);

        usersRef.child(FCMToken).child("nearPois").push().setValue(data);

    }

    private void erasePoi(){
        Resources res = getResources();
        titletext.setText(res.getText(R.string.main_texttitle));
        mImageView.setImageBitmap(null);
        showPoi.setVisibility(View.GONE);
        nearPoi = null;
        distanceLabel.setText("");

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_showpoi:
                //Αυξανεται στην βαση το πεδιο views κατα ενα οταν πατιεται το κουμπι showpoi
                updateFirView();

                //Αλλαγη activity και μεταφορα του αντικειμενου nearPoi στο καινουργιο activity
                Intent goToPreviewPoi = new Intent(this,PreviewPoi.class);
                goToPreviewPoi.putExtra("poi", (Serializable) nearPoi);
                goToPreviewPoi.putExtra("FCMToken", FCMToken);
                startActivity(goToPreviewPoi);
                break;
            default:
                break;
        }
    }

    void updateFirView(){

        myRef.child(String.valueOf(nearPoi.getId())).child("views").setValue(nearPoi.getViews() + 1);
        int newviewFir = Integer.parseInt(viewsFir) +1 ;
        usersRef.child(FCMToken).child("views").setValue(String.valueOf(newviewFir));

    }

    private void firUserRef(){
        usersRef.child(FCMToken).child("views").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                viewsFir = dataSnapshot.getValue(String.class);

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value

            }
        });
    }

    public void checkPermision(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUESTCODE);
        }else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,
                    (LocationListener) this);
        }
    }
}
