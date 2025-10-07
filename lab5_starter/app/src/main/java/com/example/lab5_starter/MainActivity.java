package com.example.lab5_starter;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AddCityFragment.AddCityDialogListener {

    private ArrayList<City> dataList;
    private ListView cityList;
    private ArrayAdapter<City> cityAdapter;
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    public void addCity(City city) {
        dataList.add(city);
        cityAdapter.notifyDataSetChanged();

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

    @Override
    public void editCity(City city, String oldName) {
        // this method also had to be modified to integrate the firestore database
        // the way I handle this is: keep old id, overwrite that with the new data, and then delete the old doc if the name changes
        cityAdapter.notifyDataSetChanged();

        // save under the new name
        DocumentReference newDocRef = citiesRef.document(city.getName());
        newDocRef.set(city);

        // if the name changed, delete the old doc
        if (!city.getName().equals(oldName)) {
            citiesRef.document(oldName).delete();
        }
    }

    // new method for deleting cities here!
    @Override
    public void deleteCity(City city) {
        dataList.remove(city);
        cityAdapter.notifyDataSetChanged();

        // delete from Firestore
        citiesRef.document(city.getName()).delete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        dataList = new ArrayList<>();
        cityList = findViewById(R.id.city_list);
        cityAdapter = new CityArrayAdapter(this, dataList);
        cityList.setAdapter(cityAdapter);

        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()){
                dataList.clear();
                for (QueryDocumentSnapshot snapshot : value){
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");

                    dataList.add(new City(name, province));
                }
                cityAdapter.notifyDataSetChanged();
            }
        });

        // add a new city
        FloatingActionButton fab = findViewById(R.id.button_add_city);
        fab.setOnClickListener(v -> {
            new AddCityFragment().show(getSupportFragmentManager(), "Add City");
        });

        // edit a city when clicked using on item click listener
        cityList.setOnItemClickListener((parent, view, position, id) -> {
            City city = dataList.get(position);
            AddCityFragment.newInstance(city).show(getSupportFragmentManager(), "Edit City");
        });

        // delete an existing city by long pressing and then giving the user a little confirmation popup
        cityList.setOnItemLongClickListener((parent, view, position, id) -> {
            City city = dataList.get(position);

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete City")
                    .setMessage("Are you sure you want to delete " + city.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dataList.remove(city);
                        cityAdapter.notifyDataSetChanged();

                        citiesRef.document(city.getName()).delete();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }
}