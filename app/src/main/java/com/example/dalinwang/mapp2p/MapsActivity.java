package com.example.dalinwang.mapp2p;

import android.app.DialogFragment;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import android.widget.Toast;
import android.widget.ImageButton;
import com.google.android.gms.maps.CameraUpdateFactory;


public class MapsActivity extends ActionBarActivity {
    Collection<String> ip_address_list = new HashSet<String>();

    TextView  infoip, infoport;
    EditText editTextAddress, editTextPort;
    ServerSocket serverSocket;
    private final static String TAG = "MapP2P";
    private static final int SocketServerPORT = 50505;
    private String response;
    private static final String request_peers = "S";
    private static final String request_peers_ack = "REQUEST_PEERS_ACK";
    private static final String send_peers_info = "SEND_PEERS_INFO";
    private static final String send_new_connection_info = "SEND_NEW_CONNECTION_INFO";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    Boolean droppingPins = false;
    FrameLayout fram_map;
    Boolean drawingLine = false; // when true, map camera movement is disabled
    final List<Marker> markers = new ArrayList<Marker>();
    final List<Polyline> polylines = new ArrayList<Polyline>();
    final List<Integer> actionsPerformed = new ArrayList<Integer>();  // 0 = drop pin, 1 = draw line

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        infoip = (TextView) findViewById(R.id.infoip);
        infoport = (TextView) findViewById(R.id.infoport);
        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        //editMessage = (EditText) findViewById(R.id.msg);


        fram_map = (FrameLayout) findViewById(R.id.fram_map);
        infoip.setText(getIpAddress());
        mMap.setOnMarkerClickListener(
                new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        marker.setSnippet("location: " + marker.getPosition().toString());
                        marker.showInfoWindow();
                        return false;
                    }
                }
        );

        mMap.setOnMarkerDragListener(
                new GoogleMap.OnMarkerDragListener() {
                    @Override
                    public void onMarkerDragStart(Marker marker) {
                        System.out.println("Start position:" + marker.getPosition().toString());
                    }

                    @Override
                    public void onMarkerDrag(Marker marker) {

                    }

                    @Override
                    public void onMarkerDragEnd(Marker marker) {
                        System.out.println("End position:" + marker.getPosition().toString());
                    }
                }
        );

        if (fram_map != null) {
            fram_map.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (drawingLine) {
                        float x = event.getX();
                        float y = event.getY();

                        int x_int = Math.round(x);
                        int y_int = Math.round(y);

                        Point x_y_point = new Point(x_int, y_int);

                        LatLng latLng = mMap.getProjection().fromScreenLocation(x_y_point);
                        List<LatLng> points;

                        double latitude = latLng.latitude;
                        double longitude = latLng.longitude;
                        int lineNum = polylines.size() - 1;

                        int action = event.getAction();
                        switch (action) {
                            case MotionEvent.ACTION_DOWN:
                                // finger touches the screen

                                PolylineOptions lineOption = new PolylineOptions().color(Color.RED);
                                //lineOptions.add(lineOption);
                                Polyline polyline = mMap.addPolyline(lineOption);
                                polylines.add(polyline);
                                break;
                            case MotionEvent.ACTION_MOVE:
                                // finger moves on the screen

                                //lineOptions.get(lineNum).add(latLng);

                                points = polylines.get(lineNum).getPoints();
                                points.add(latLng);
                                polylines.get(lineNum).setPoints(points);
                                break;
                            case MotionEvent.ACTION_UP:
                                // finger leaves the screen
                                //mMap.addPolyline(lineOptions);
                                //mMap.addMarker(new MarkerOptions().position(latLng).title("Marker")).setDraggable(true);
                                //Polyline polyline = mMap.addPolyline(lineOptions.get(lineNum));

                                // send to client/server

                                actionsPerformed.add(1);
                                List<LatLng> current_pts = polylines.get(lineNum).getPoints();
                                String line_msg = "l:";
                                for (LatLng pt : current_pts) {
                                    String line_msg_latitude = new Double(pt.latitude).toString();
                                    String line_msg_longitude = new Double(pt.longitude).toString();
                                    line_msg = line_msg + (";" + line_msg_latitude + "," + line_msg_longitude);
                                }

                                // iterate through peer list
                                for(String address : ip_address_list) {
                                    if(!address.equals(getIpAddress())) {
                                        MyClientTask myClientTask = new MyClientTask(
                                                address,
                                                SocketServerPORT);
                                        myClientTask.execute(line_msg);
                                    }
                                }
                                /*
                                MyClientTask myClientTask = new MyClientTask(
                                        editTextAddress.getText().toString(),
                                        Integer.parseInt(editTextPort.getText().toString()));
                                myClientTask.execute(line_msg);
                                */
                                Log.d(TAG, "new action performed: draw line");
                                Log.d(TAG, "Number of lines is:");
                                Log.d(TAG, Integer.toString(polylines.size()));
                                Log.d(TAG, "Number of actions performed is:");
                                Log.d(TAG, Integer.toString(actionsPerformed.size()));

                                break;
                        }
                    }
                    if (drawingLine == true) {
                        return true;

                    } else {
                        return false;
                    }
                }
            });

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serverSocket != null) {
            try {
                serverSocket.close();
                Log.i(TAG, "server socket closed in onPause");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SocketServerThread extends Thread {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                MapsActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoport.setText("Host Port Number: "
                                + serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    Socket socket = serverSocket.accept();
                    response = "";
                    ByteArrayOutputStream byteArrayOutputStream =
                            new ByteArrayOutputStream(48576);
                    byte[] buffer = new byte[48576];

                    int bytesRead;
                    InputStream inputStream = socket.getInputStream();
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                        response += byteArrayOutputStream.toString("UTF-8");

                    }
                    final String newAddress = socket.getInetAddress().toString().substring(1);

                    if (response.equals(request_peers)) {
                        // send ack to new connection
                        MyClientTask myClientTask = new MyClientTask(
                                socket.getInetAddress().toString().substring(1),
                                SocketServerPORT);
                        myClientTask.execute(request_peers_ack);


                        if (ip_address_list.size() != 0 && !newAddress.equals(getIpAddress())) {
                            for (String address : ip_address_list) {
                                if (!address.equals(newAddress)) {
                                    //send existing peers info to new connection
                                    MyClientTask send_to_new_connection = new MyClientTask(
                                            socket.getInetAddress().toString().substring(1),
                                            SocketServerPORT);
                                    String peers_info_msg = send_peers_info + ":" + address;
                                    send_to_new_connection.execute(peers_info_msg);

                                    //send new connection to existing peers
                                    MyClientTask send_to_existing_connections = new MyClientTask(
                                            address, SocketServerPORT);
                                    String new_connection_info = send_new_connection_info + ":" + newAddress;
                                    send_to_existing_connections.execute(new_connection_info);

                                }
                            }
                        }


                        //add new connection to ip_address_list
                        if (!ip_address_list.contains(newAddress) && !newAddress.equals(getIpAddress())) {
                            ip_address_list.add(socket.getInetAddress().toString().substring(1));
                            MapsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String toast_msg = "New User Connected!";
                                    Toast.makeText(getApplicationContext(), toast_msg, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    } else if (response.equals(request_peers_ack)) {
                        if (!socket.getInetAddress().toString().substring(1).equals(getIpAddress())
                                && !ip_address_list.contains(socket.getInetAddress().toString().substring(1))) {
                            ip_address_list.add(socket.getInetAddress().toString().substring(1));
                            MapsActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String toast_msg = "Connection Successful!";
                                    Toast.makeText(getApplicationContext(), toast_msg, Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    } else if (response.contains(send_peers_info)) {
                        String peerAddress = response.split(":")[1];
                        ip_address_list.add(peerAddress);


                    } else if (response.contains(send_new_connection_info)) {
                        String new_connection_address = response.split(":")[1];
                        ip_address_list.add(new_connection_address);
                    } else if (response.contains("p:")) {
                        //String temp[] = response.split(":");
                        MapsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MapsActivity.this.dropPin2(response.split(":")[1]);
                            }
                        });

                    } else if (response.contains("l:")) {
                        MapsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MapsActivity.this.receiveLine(response.split(":")[1].substring(1));
                            }
                        });
                    }


                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }

    //Client side

    public void showIPDialog(View v) {
        DialogFragment newFragment = new IPDialogFragment();
        newFragment.show(getFragmentManager(), "Enter IP Address");
    }

    // Run when Connect button is clicked in IP Dialog
    public void connect(String address, int port) {
        //ip_address_list.add(editTextAddress.getText().toString());
        MyClientTask myClientTask = new MyClientTask(address, port);
        myClientTask.execute(request_peers);
    }

    public class MyClientTask extends AsyncTask<String, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";

        MyClientTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
        }

        @Override
        protected Void doInBackground(String... messages) {

            Socket socket = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                OutputStream outputStream = socket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                if (messages != null) {
                    printStream.print(messages[0]);
                }
                printStream.close();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

    }

    private String getIpAddress() {
        String ip = "";
        int counter = 0;
        boolean hasFound = false;
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements() && !hasFound) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements() && !hasFound) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    //if (inetAddress.isSiteLocalAddress()) {
                    if (counter == 1) {
                        counter++;
                        ip += "SiteGlobalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                        hasFound = true;
                    } else {
                        counter++;
                        //ip += "NotFoundLocalAddress\n";
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handle presses on the action bar items
        switch (id) {
            case R.id.action_droppin:
                drawingLine = false;
                if (droppingPins != true) {
                    droppingPins = true;
                    dropPin();
                } else {
                    droppingPins = false;
                }
                return true;
            case R.id.action_drawline:
                droppingPins = false;
                if (drawingLine != true) {
                    drawingLine = true;
                    Log.d(TAG, "drawing mode: on");
                } else {
                    drawingLine = false;
                    Log.d(TAG, "drawing mode: off");
                }
                return true;
            case R.id.action_undo:
                if (actionsPerformed.size() > 0) {
                    undo();

                    /*
                    String undo_msg = "u:";
                    MyClientTask myClientTask = new MyClientTask(
                            editTextAddress.getText().toString(),
                            Integer.parseInt(editTextPort.getText().toString()));
                    myClientTask.execute(undo_msg);*/
                }
                return true;
            case R.id.action_deleteall:
                for (int i = markers.size() - 1; i >= 0; i--) {
                    markers.get(i).remove();
                    markers.remove(i);
                    actionsPerformed.remove(i);
                    Log.d(TAG, "removing pin");
                    Log.d(TAG, "Number of pins is:");
                    Log.d(TAG, Integer.toString(markers.size()));
                    Log.d(TAG, "Number of actions performed is:");
                    Log.d(TAG, Integer.toString(actionsPerformed.size()));
                }
                for (int i = polylines.size() - 1; i >= 0; i--) {
                    polylines.get(i).remove();
                    polylines.remove(i);
                    actionsPerformed.remove(i);
                    Log.d(TAG, "removing line");
                    Log.d(TAG, "Number of lines is:");
                    Log.d(TAG, Integer.toString(polylines.size()));
                    Log.d(TAG, "Number of actions performed is:");
                    Log.d(TAG, Integer.toString(actionsPerformed.size()));
                }
                for (int i = 0; i < actionsPerformed.size(); i++) {
                    Log.d(TAG, "apparently you missed one");
                    actionsPerformed.set(i, 0);
                }
                return true;
            case R.id.action_help:
                //openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void undo() {
        if(actionsPerformed.get(actionsPerformed.size()-1)==0)
        {
            markers.get(markers.size() - 1).remove();
            markers.remove(markers.size() - 1);
            actionsPerformed.remove(actionsPerformed.size() - 1);
            Log.d(TAG, "last action removed: drop pin");
            Log.d(TAG, "Number of pins is:");
            Log.d(TAG, Integer.toString(markers.size()));
            Log.d(TAG, "Number of actions performed is:");
            Log.d(TAG, Integer.toString(actionsPerformed.size()));
        }

        else if(actionsPerformed.get(actionsPerformed.size()-1)==1)
        {
            polylines.get(polylines.size() - 1).remove();
            polylines.remove(polylines.size() - 1);
            actionsPerformed.remove(actionsPerformed.size() - 1);
            Log.d(TAG, "last action removed: draw line");
            Log.d(TAG, "Number of lines is:");
            Log.d(TAG, Integer.toString(polylines.size()));
            Log.d(TAG, "Number of actions performed is:");
            Log.d(TAG, Integer.toString(actionsPerformed.size()));
        }
    }


    public void dropPin() {
        mMap.setOnMapClickListener(
                new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng point) {
                        if(droppingPins) {
                            Marker marker = mMap.addMarker(new MarkerOptions().position(point).title("Marker"));
                            marker.setDraggable(true);
                            markers.add(marker);
                            actionsPerformed.add(0);

                            // iterate through peer list
                            for(String address : ip_address_list){
                                MyClientTask myClientTask = new MyClientTask(
                                        address,
                                        SocketServerPORT);
                                String pin_msg_latitude = new Double(point.latitude).toString();
                                String pin_msg_longitude = new Double(point.longitude).toString();
                                String pin_msg = "p:" + pin_msg_latitude + "," + pin_msg_longitude;

                                myClientTask.execute(pin_msg);
                            }
                            /*
                            MyClientTask myClientTask = new MyClientTask(
                                    editTextAddress.getText().toString(),
                                    Integer.parseInt(editTextPort.getText().toString()));
                            String pin_msg_latitude = new Double(point.latitude).toString();
                            String pin_msg_longitude = new Double(point.longitude).toString();
                            String pin_msg = "p:" + pin_msg_latitude + "," + pin_msg_longitude;
                            myClientTask.execute(pin_msg);
                            */
                            Log.d(TAG, "new action performed: drop pin");
                            Log.d(TAG, "Number of pins is:");
                            Log.d(TAG, Integer.toString(markers.size()));
                            Log.d(TAG, "Number of actions performed is:");
                            Log.d(TAG, Integer.toString(actionsPerformed.size()));
                        }
                    }
                }
        );
    }

    public void dropPin2(String Lat_lng){
        String latlng [] = Lat_lng.split(",");
        double latitude = Double.parseDouble(latlng[0]);
        double longitude = Double.parseDouble(latlng[1]);
        //infoport.setText("latitude is " + latitude);
        //infoip.setText("longitude is " + longitude);

        Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Marker"));
        marker.setDraggable(true);
        markers.add(marker);
        actionsPerformed.add(0);
    }

    public void receiveLine(String line_lat_lng){

        String latlng_pairs [] = line_lat_lng.split(";");

        PolylineOptions line_options = new PolylineOptions().color(Color.BLUE);
        Polyline polyline = mMap.addPolyline(line_options);
        List<LatLng> points = new ArrayList<>();

        for(int i = 0; i < latlng_pairs.length - 1; i++){
            String lat_lng [] = latlng_pairs[i].split(",");

            double latitude = Double.parseDouble(lat_lng[0].replace("l", ""));

            double longitude = Double.parseDouble(lat_lng[1].replace("l", ""));
            LatLng pt = new LatLng(latitude, longitude);
            points.add(pt);
        }
        polyline.setPoints(points);
        this.polylines.add(polyline);
        actionsPerformed.add(1);
    }




    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(34.4125, -119.8481), 13));
        mMap.addMarker(new MarkerOptions().position(new LatLng(34.4125, -119.8481)).title("UCSB")).setDraggable(true);

    }
}
