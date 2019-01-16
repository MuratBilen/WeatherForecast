package com.bilen.murat.weatherforecast;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class WeatherController extends AppCompatActivity
{

	final int REQUEST_CODE = 123;
	String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";

	final String APP_ID = "ddc038c18a91b85e8d1dead7ce9ba0ae";


	// Time between location updates (5000 milliseconds or 5 seconds)
	final long MIN_TIME = 5000;
	// Distance between location updates (1000m or 1km)
	final float MIN_DISTANCE = 1000;

	LocationManager mLocationManager;
	LocationListener mLocationListener;


	// Member Variables:
	String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;
	TextView mTemperatureLabel;

	TextView mCityLabel;
	ImageView mWeatherImage;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(com.bilen.murat.weatherforecast.R.layout.weather_controller_layout);

		// Linking the elements in the layout to Java code

		mCityLabel = (TextView) findViewById(com.bilen.murat.weatherforecast.R.id.locationTV);
		mWeatherImage = (ImageView) findViewById(com.bilen.murat.weatherforecast.R.id.weatherSymbolIV);
		mTemperatureLabel = (TextView) findViewById(com.bilen.murat.weatherforecast.R.id.tempTV);
		ImageButton changeCityButton = (ImageButton) findViewById(com.bilen.murat.weatherforecast.R.id.changeCityButton);


		changeCityButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent myIntent = new Intent(WeatherController.this, ChangeCityController.class);
				startActivity(myIntent);
			}
		});

	}


	protected void onResume()
	{
		super.onResume();
		Log.d("WeatherForecast", "onResume() called");
		Intent myIntent = getIntent();
		String city = myIntent.getStringExtra("City");
		if (city != null) {
			getWeatherForNewCity(city);
		}

		Log.d("WeatherForecast", "Getting weather for current location");
		getWeatherForCurrentLocation();
	}

	private void getWeatherForNewCity(String city)
	{
		RequestParams params = new RequestParams();
		params.put("q", city);
		params.put("appid", APP_ID);
		letsDoSomeNetworking(params);
	}

	private void getWeatherForCurrentLocation()
	{
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new LocationListener()
		{
			@Override
			public void onLocationChanged(Location location)
			{
				Log.d("WeatherForecast", "onLocationChanged() callback received");
				String longitude = String.valueOf(location.getLongitude());
				String latitude = String.valueOf(location.getLatitude());
				Log.d("WeatherForecast", "longitude is: " + longitude);
				Log.d("WeatherForecast", "latitude is: " + latitude);
				RequestParams params = new RequestParams();
				params.put("lat", latitude);
				params.put("lon", longitude);
				params.put("appid", APP_ID);
				letsDoSomeNetworking(params);
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras)
			{

			}

			@Override
			public void onProviderEnabled(String provider)
			{

			}

			@Override
			public void onProviderDisabled(String provider)
			{
				Log.d("WeatherForecast", "onProviderDisabled() callback received");
			}
		};
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);

			return;
		}
		Intent myIntent = getIntent();
		String city = myIntent.getStringExtra("City");
		if (city == null) {
			mLocationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, mLocationListener);
		}


	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.d("WeatherForecast", "onRequestPermissionResult(): Permission granted!");
				getWeatherForCurrentLocation();
			} else {
				Log.d("WeatherForecast", "Permission denied!");
			}

		}
	}


	private void letsDoSomeNetworking(RequestParams params)
	{
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(WEATHER_URL, params, new JsonHttpResponseHandler()
		{
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response)
			{
				Log.d("WeatherForecast", "Success! JSON: " + response.toString());
				WeatherDataModel weatherData = WeatherDataModel.fromJson(response);
				updateUI(weatherData);
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response)
			{
				Log.e("WeatherForecast", "Fail" + e.toString());
				Log.d("WeatherForecast", "Staus code" + statusCode);
				Toast.makeText(WeatherController.this, "Request Failed", Toast.LENGTH_SHORT).show();
			}
		});
	}


	// Update function
	private void updateUI(WeatherDataModel weather)
	{
		mTemperatureLabel.setText(weather.getmTemperature());
		mCityLabel.setText(weather.getmCity());
		int resourseID = getResources().getIdentifier(weather.getmIconName(), "drawable", getPackageName());
		mWeatherImage.setImageResource(resourseID);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mLocationManager != null)
			mLocationManager.removeUpdates(mLocationListener);

	}


}
