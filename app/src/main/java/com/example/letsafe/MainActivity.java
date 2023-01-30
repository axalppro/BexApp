package com.example.letsafe;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    private static final int REQUEST_CONTACT = 1;
    int ALL_PERMISSIONS = 101;
    final String[] permissions = new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    String position = new String();

    private Collection<ContactInfo> contacts;
    Button btn_bracelet, btn_AddContact1;
    TextView print, lbl_contact1;
    TextView txt_contacts;
    ContactsContract contact_info;
    CheckBox bx_location;
    EditText txt_msg;

    /**************************************************************************************************************************************************************
     * Initialization
     * ************************************************************************************************************************************************************
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contacts = new ArrayList<>();
        contacts.add(new ContactInfo("ME -> test", "+41798414503"));

        //Linking UI
        btn_bracelet = (Button) findViewById(R.id.btn_Bracelet);
        btn_AddContact1 = findViewById(R.id.btn_AddContact1);
        print = (TextView) findViewById(R.id.tv_print);
        txt_contacts = findViewById(R.id.txt_contacts);
        bx_location = findViewById(R.id.bx_location);
        txt_msg = findViewById(R.id.txt_msg);

        btn_bracelet.setEnabled(false); //Disable while permissions not granted
        updateContactsView();

        /**************************************************************************************************************************************************************
         * OnClick Listeners definition
         * ************************************************************************************************************************************************************
         */
        btn_bracelet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                react_to_bracelet();
            }
        });

        btn_AddContact1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(contactPickerIntent, REQUEST_CONTACT);
                //registerForActivityResult(contactPickerIntent, REQUEST_CONTACT);
            }
        });

        /**
         * Register for location update
         */

        FusedLocationProviderClient fusedLocationClient = getFusedLocationProviderClient(this.getApplicationContext());

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data

                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);


        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            //Continue
            Toast.makeText(getApplicationContext(), "SMS already authorized", Toast.LENGTH_LONG).show();
            btn_bracelet.setEnabled(true);
        } else {
            permissionsRequest.launch(permissions);
            //requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }

    }

    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            //Nothing to do
            btn_bracelet.setEnabled(true);
            Toast.makeText(getApplicationContext(), "SMS authorized", Toast.LENGTH_LONG).show();

        } else {
            //Do something
            Toast.makeText(getApplicationContext(), "SMS not authorized", Toast.LENGTH_LONG).show();
        }
    });

    ActivityResultLauncher<String[]> permissionsRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    // Precise location access granted.
                    Toast.makeText(getApplicationContext(), "Fine Location authorized", Toast.LENGTH_LONG).show();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // Only approximate location access granted.
                    Toast.makeText(getApplicationContext(), "Approximate Location authorized", Toast.LENGTH_LONG).show();
                } else {
                    // No location access granted.
                    Toast.makeText(getApplicationContext(), "Location not authorized", Toast.LENGTH_LONG).show();
                }

                if (result.getOrDefault(Manifest.permission.SEND_SMS, false)) {
                    btn_bracelet.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "SMS authorized", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(getApplicationContext(), "SMS not authorized", Toast.LENGTH_LONG).show();
                }

            }
    );

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CONTACT:
                    Cursor cursor = null;
                    try {
                        String phoneNo = null;
                        String name = null;

                        Uri uri = data.getData();
                        cursor = getContentResolver().query(uri, null, null, null, null);
                        cursor.moveToFirst();
                        int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        phoneNo = cursor.getString(phoneIndex);
                        name = cursor.getString(nameIndex);

                        System.out.println("Name and Contact number is " + name + "," + phoneNo);
                        contacts.add(new ContactInfo(name, phoneNo));
                        updateContactsView();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            System.out.println("Failed, Not able to pick contact");
        }
    }

    protected void send_message()
    {
        try
        {
            SmsManager smgr = SmsManager.getDefault();
            String theMsg = new String();

            // Get the FusedLocationProviderClient
            FusedLocationProviderClient fusedLocationClient = getFusedLocationProviderClient(this);


            // Get the last known location
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {


                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    // Do something with the location
                                    position = "N"+String.valueOf(location.getLatitude())+ " E" + String.valueOf(location.getLongitude());
                                }
                                else
                                {
                                    position = "Invalid position";
                                }
                            }
                        });
            }

            theMsg = txt_msg.getText().toString() + (bx_location.isChecked()?("\n\nMy location is : "+ position):"No location access");

            for (ContactInfo c : contacts) {
                smgr.sendTextMessage(c.getPhoneNumber(),null,theMsg,null,null);
            }
            Toast.makeText(MainActivity.this, "SMS Sent Successfully", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            print.setText(e.getMessage());
            Toast.makeText(MainActivity.this, "SMS Failed to Send, Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    /**************************************************************************************************************************************************************
     * Send a SMS
     * ************************************************************************************************************************************************************
     */
    protected void react_to_bracelet() {

        //Ask for last location

    }

    protected void updateContactsView()
    {
        String temp = "No contact added";
        if(!contacts.isEmpty())
        {
            temp = "Send alert to : \n";
            for (ContactInfo c: contacts) {
                temp = temp + c.getName() + "\n";
            }
        }

        txt_contacts.setText(temp);

    }
}