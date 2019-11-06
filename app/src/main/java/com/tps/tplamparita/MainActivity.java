package com.tps.tplamparita;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.hardware.camera2.*;
import android.widget.NumberPicker;

public class MainActivity extends AppCompatActivity {

    ImageView imgLamparita;
    ImageButton imgbtnInterruptor;
    CheckBox chkParpadeo;
    NumberPicker npkIntervalo;
    Handler handler;
    Runnable runnable = null;
    SoundPool sonidito;
    SharedPreferences preferencias;
    int iSoundId;
    boolean bPrendido;
    boolean bLuzPrendida;
    boolean bTitilando;
    int iIntervalo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        agregarReferencias();
        setearListeners();
        init();
    }

    private void init() {
        handler = new Handler();
        preferencias = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
        bPrendido = false;
        bLuzPrendida = false;
        bTitilando = preferencias.getBoolean("CheckboxTitilando", false);
        iIntervalo = preferencias.getInt("Intervalo", 0);
        npkIntervalo.setEnabled(false);
        npkIntervalo.setMinValue(1);
        npkIntervalo.setMaxValue(20);
        sonidito = new SoundPool.Builder().setMaxStreams(10).build();
        iSoundId = sonidito.load(getApplicationContext(), R.raw.cebo, 1);
        if (bTitilando) {
            npkIntervalo.setEnabled(true);
            chkParpadeo.setChecked(true);
            if (iIntervalo != 0) {
                npkIntervalo.setValue(iIntervalo);
            }
        }
    }

    private void setearListeners() {
        imgbtnInterruptor.setOnClickListener(interruptor_click);
        chkParpadeo.setOnCheckedChangeListener(chkparpadeo_changed);
        npkIntervalo.setOnValueChangedListener(npkIntervalo_changed);
    }

    private void agregarReferencias() {
        imgLamparita = (ImageView) findViewById(R.id.imgLamparita);
        imgbtnInterruptor = (ImageButton) findViewById(R.id.imgbtnInterruptor);
        chkParpadeo = (CheckBox) findViewById(R.id.chkParpadeo);
        npkIntervalo = (NumberPicker) findViewById(R.id.npkIntervalo);
    }

    NumberPicker.OnValueChangeListener npkIntervalo_changed = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            iIntervalo = newVal;
            SharedPreferences.Editor editor = preferencias.edit();
            editor.putInt("Intervalo", iIntervalo);
            editor.commit();
            if (runnable != null) {
                handler.removeCallbacks(runnable);
                handler.postAtTime(runnable, System.currentTimeMillis()+iIntervalo*1000);
                handler.postDelayed(runnable, iIntervalo*1000);
            }
        }
    };


    View.OnClickListener interruptor_click = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {
            if (!bPrendido) {
                bPrendido = true;
                if (!bTitilando) {
                    bLuzPrendida = true;
                    handler.removeCallbacks(runnable);
                    SetLuz(bLuzPrendida);
                } else {
                    runnable = new Runnable(){
                        public void run() {
                            bLuzPrendida = !bLuzPrendida;
                            SetLuz(bLuzPrendida);
                        }
                    };
                    handler.postAtTime(runnable, System.currentTimeMillis()+iIntervalo*1000);
                    handler.postDelayed(runnable, iIntervalo*1000);
                }
            } else {
                bPrendido = false;
                bTitilando = false;
                bLuzPrendida = false;
                handler.removeCallbacks(runnable);
                SetLuz(bPrendido);
            }

        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        void SetLuz(boolean prendido) {
            ReproducirSonido();
            Context context = getApplicationContext();
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
                CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    String cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, prendido);
                    int imagen = (prendido)? R.drawable.prendido : R.drawable.apagado;
                    imgbtnInterruptor.setImageResource(imagen);
                } catch (CameraAccessException e) {
                    Log.d("Error", "No hay acceso a la camara");
                }
            }
        }
    };

    private void ReproducirSonido() {
        sonidito.play(iSoundId, 1, 1, 1, 0, 1);
    }

    CompoundButton.OnCheckedChangeListener chkparpadeo_changed = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                AlertDialog mensajito = new AlertDialog.Builder(getApplicationContext())
                        .setTitle("Confirmar check")
                        .setMessage("Estas seguro que queres checkear este coso?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                bTitilando = true;
                                npkIntervalo.setEnabled(true);
                                SharedPreferences.Editor editor = preferencias.edit();
                                editor.putBoolean("CheckboxTitilando", true);
                                editor.commit();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            } else {
                bTitilando = false;
                npkIntervalo.setEnabled(false);
                SharedPreferences.Editor editor = preferencias.edit();
                editor.putBoolean("CheckboxTitilando", false);
                editor.commit();
            }

        }
    };
}
