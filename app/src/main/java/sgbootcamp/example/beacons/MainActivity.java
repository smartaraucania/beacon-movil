

package sgbootcamp.example.beacons;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.CurveFittedDistanceCalculator;
import org.altbeacon.beacon.distance.DistanceCalculator;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.io.Console;
import java.util.ArrayList;
import java.util.Collection;

import static sgbootcamp.example.beacons.App.CHANNEL_1_ID;

/**
 * App main activity, gestiona la detección de beacons, mostrando un mensaje de los beacons
 * detectados
 *
 * @author David González Verdugo
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, BeaconConsumer,
        RangeNotifier {

    //-----------------Beacon variables--------------------
    protected final String TAG = MainActivity.this.getClass().getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final long DEFAULT_SCAN_PERIOD_MS = 6000l;
    private static final String ALL_BEACONS_REGION = "AllBeaconsRegion";
    private BeaconManager mBeaconManager; // Para interactuar con los beacons desde una actividad
    private Region mRegion; // Representa el criterio de campos con los que buscar beacons
    //-----------------Beacon variables--------------------

    //-----------------Notificacion variables--------------------
//    private PendingIntent pendingIntent;
//    private final static String CHANNEL_ID = "NOTIFICACIÓN";
//    public final static int NOTIFY_ID = 0;

    private NotificationManagerCompat notificationManagerCompat;
    private EditText title_edittext;
    private EditText message_edittext;
    //-----------------Notificacion variables--------------------

    //-----------------List variables--------------------
    ListView beaconList;
    ArrayList<BeaconModel> beaconArrayList = new ArrayList<BeaconModel>();
    private Adaptador adaptador;
    //-----------------List variables--------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconList = (ListView) findViewById(R.id.beacons_list);

        notificationManagerCompat = NotificationManagerCompat.from(this);
        title_edittext = findViewById(R.id.title_edittext);
        message_edittext = findViewById(R.id.message_edittext);

        getStartButton().setOnClickListener(this);
        getStopButton().setOnClickListener(this);

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());

        // Fijar un protocolo beacon, Eddystone en este caso
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));

        //mBeaconManager.bind(this);

        ArrayList<Identifier> identifiers = new ArrayList<>();

        mRegion = new Region(ALL_BEACONS_REGION, identifiers);
    }

    @Override
    public void onClick(View view) {

        if (view.equals(findViewById(R.id.startReadingBeaconsButton))) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Si los permisos de localización todavía no se han concedido, solicitarlos
                if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {

                    askForLocationPermissions();

                } else { // Permisos de localización concedidos

                    prepareDetection();
                }

            } else { // Versiones de Android < 6

                prepareDetection();
            }

        } else if (view.equals(findViewById(R.id.stopReadingBeaconsButton))) {

            stopDetectingBeacons();

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            // Desactivar bluetooth
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
        }
    }

    /**
     * Activar localización y bluetooth para empezar a detectar beacons
     */
    private void prepareDetection() {

        if (!isLocationEnabled()) {

            askToTurnOnLocation();

        } else { // Localización activada, comprobemos el bluetooth

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {

                showToastMessage(getString(R.string.not_support_bluetooth_msg));

            } else if (mBluetoothAdapter.isEnabled()) {

                startDetectingBeacons();

            } else {

                // Pedir al usuario que active el bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            // Usuario ha activado el bluetooth
            if (resultCode == RESULT_OK) {

                startDetectingBeacons();

            } else if (resultCode == RESULT_CANCELED) { // User refuses to enable bluetooth

                showToastMessage(getString(R.string.no_bluetooth_msg));
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Empezar a detectar los beacons, ocultando o mostrando los botones correspondientes
     */
    private void startDetectingBeacons() {

        // Fijar un periodo de escaneo
        mBeaconManager.setForegroundScanPeriod(DEFAULT_SCAN_PERIOD_MS);

        // Enlazar al servicio de beacons. Obtiene un callback cuando esté listo para ser usado
        mBeaconManager.bind(this);

        // Desactivar botón de comenzar
        getStartButton().setEnabled(false);
        getStartButton().setAlpha(.5f);

        // Activar botón de parar
        getStopButton().setEnabled(true);
        getStopButton().setAlpha(1);
    }

    @Override
    public void onBeaconServiceConnect() {

        try {
            // Empezar a buscar los beacons que encajen con el el objeto Región pasado, incluyendo
            // actualizaciones en la distancia estimada
            mBeaconManager.startRangingBeaconsInRegion(mRegion);

            showToastMessage(getString(R.string.start_looking_for_beacons));

        } catch (RemoteException e) {
            Log.d(TAG, "Se ha producido una excepción al empezar a buscar beacons " + e.getMessage());
        }

        mBeaconManager.addRangeNotifier(this);
    }


    /**
     * Método llamado cada DEFAULT_SCAN_PERIOD_MS segundos con los beacons detectados durante ese
     * periodo
     */

    boolean duplicate = false;
    int cont = 0;

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

        if (beacons.size() != 0) {

            try {
                for (Beacon beacon : beacons) {
                    //String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                    //showToastMessage("Temeletry data "+ beacon.getExtraDataFields().get(1));
                    double distance = fijarNumero(beacon.getDistance(), 3);

                    if (beaconArrayList.size() > 1) {
                        for (BeaconModel ba : beaconArrayList) {
                            if (ba.getInstanceID().equals(beacon.getId2().toString())) {
                                ba.setDistanceID(distance + " m");
                                ba.setRssiId(beacon.getRssi() + " dBm");
                                ba.setTxId(beacon.getTxPower() + " dBm");
                                ba.setBateryId(beacon.getExtraDataFields().get(1) + " mV");
                                duplicate = true;
                                //System.out.println("duplicado" + cont++);
                            }
                        }
                    }

                    if (duplicate) {
                        adaptador.notifyDataSetChanged();
                        duplicate = false;
                    } else {
                        beaconArrayList.add(new BeaconModel(beacon.getId1().toString(), beacon.getId2().toString(),
                                beacon.getExtraDataFields().get(1) + " mV", distance + " m",
                                beacon.getRssi() + " dBm", beacon.getTxPower() + " dBm"));
                        adaptador = new Adaptador(this, beaconArrayList);
                        beaconList.setAdapter(adaptador);
                        notificationCreate();
//                        createNotifyChannel();
//                        createNotify();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
        } else {
            showToastMessage(getString(R.string.no_beacons_detected));
        }
    }

    public void notificationCreate() {
        String title = title_edittext.getText().toString();
        String message = message_edittext.getText().toString();

        if (title.isEmpty()) {
            title = "Se ha detectado una baliza";
        }
        if (message.isEmpty()) {
            message = "Podemos ofrecerles una promoción solo por hoy";
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_baseline_adb_24)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(Color.CYAN)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManagerCompat.notify(1, notification);
    }

//    private void createNotifyChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "Notificacion";
//            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
//            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            notificationManager.createNotificationChannel(notificationChannel);
//        }
//    }
//
//    private void createNotify() {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
//        builder.setSmallIcon(R.drawable.ic_baseline_wifi_tethering_24);
//        builder.setContentTitle("Baliza detectada");
//        builder.setContentText("Se ha detectado una baliza cerca!");
//        builder.setColor(Color.CYAN);
//        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
//        builder.setLights(Color.MAGENTA, 1000, 1000);
//        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
//        builder.setDefaults(Notification.DEFAULT_SOUND);
//        builder.setContentIntent(pendingIntent);
//
//        NotificationManagerCompat nmc = NotificationManagerCompat.from(getApplicationContext());
//        nmc.notify(NOTIFY_ID, builder.build());
//        boolean areNotificationsEnabled = nmc.areNotificationsEnabled();
//        System.out.println("notificaiones activas : " + areNotificationsEnabled);
//    }

    public static double fijarNumero(double numero, int digitos) {

        if (numero < 0) {
            numero = numero * (-1);
        }

        double resultado;
        resultado = numero * Math.pow(10, digitos);
        resultado = Math.round(resultado);
        resultado = resultado / Math.pow(10, digitos);
        return resultado;
    }

    private void stopDetectingBeacons() {

        try {
            mBeaconManager.stopMonitoringBeaconsInRegion(mRegion);
            showToastMessage(getString(R.string.stop_looking_for_beacons));
        } catch (RemoteException e) {
            Log.d(TAG, "Se ha producido una excepción al parar de buscar beacons " + e.getMessage());
        }

        mBeaconManager.removeAllRangeNotifiers();

        // Desenlazar servicio de beacons
        mBeaconManager.unbind(this);

        // Activar botón de comenzar
        getStartButton().setEnabled(true);
        getStartButton().setAlpha(1);

        // Desactivar botón de parar
        getStopButton().setEnabled(false);
        getStopButton().setAlpha(.5f);
    }

    /**
     * Comprobar permisión de localización para Android >= M
     */
    private void askForLocationPermissions() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.location_access_needed);
        builder.setMessage(R.string.grant_location_access);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onDismiss(DialogInterface dialog) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    prepareDetection();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.funcionality_limited);
                    builder.setMessage(getString(R.string.location_not_granted) +
                            getString(R.string.cannot_discover_beacons));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    /**
     * Comprobar si la localización está activada
     *
     * @return true si la localización esta activada, false en caso contrario
     */
    private boolean isLocationEnabled() {

        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        boolean networkLocationEnabled = false;

        boolean gpsLocationEnabled = false;

        try {
            networkLocationEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            gpsLocationEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        } catch (Exception ex) {
            Log.d(TAG, "Excepción al obtener información de localización");
        }

        return networkLocationEnabled || gpsLocationEnabled;
    }

    /**
     * Abrir ajustes de localización para que el usuario pueda activar los servicios de localización
     */
    private void askToTurnOnLocation() {

        // Notificar al usuario
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(R.string.location_disabled);
        dialog.setPositiveButton(R.string.location_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });
        dialog.show();
    }

    private Button getStartButton() {
        return (Button) findViewById(R.id.startReadingBeaconsButton);
    }

    private Button getStopButton() {
        return (Button) findViewById(R.id.stopReadingBeaconsButton);
    }

    /**
     * Mostrar mensaje
     *
     * @param message mensaje a enseñar
     */
    private void showToastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeaconManager.removeAllRangeNotifiers();
        mBeaconManager.unbind(this);
    }
}
