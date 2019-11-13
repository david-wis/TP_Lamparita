package com.tps.tplamparita;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.hardware.camera2.*;
import android.widget.NumberPicker;
import android.widget.Toast;

public class MainActivity extends Activity {

    ImageView imgLamparita;
    ImageButton imgbtnInterruptor;
    CheckBox chkParpadeo;
    NumberPicker npkIntervalo;
    Handler handler;
    Runnable runnable = null;
    SoundPool sonidito;
    SharedPreferences preferencias;
    Context context;
    int iSoundId;
    boolean bPrendido;
    boolean bLuzPrendida;
    boolean bTitilando;
    boolean bCamaraDisponible;
    int iIntervalo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        agregarReferencias();
        init();
        setearListeners();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void init() {
        context = MainActivity.this;
        bCamaraDisponible = verificarCamara();
        if (!bCamaraDisponible) {
            AlertDialog mensajito = new AlertDialog.Builder(context)
                    .setTitle("Advertencia")
                    .setMessage("No tenes camara")
                    .setNegativeButton("Cerrar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //finishAndRemoveTask();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create();
            mensajito.show();
        }
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
        establecerTitilado();
    }

    private boolean verificarCamara() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
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
            /*if (runnable != null) {
                handler.removeCallbacks(runnable);
                //handler.postAtTime(runnable, System.currentTimeMillis()+iIntervalo*1000);
                handler.postDelayed(runnable, iIntervalo*1000);
            }*/
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
                    //handler.removeCallbacks(runnable);
                    SetLuz(bLuzPrendida);
                }/* else {
                    establecerTitilado();
                }*/
            } else {
                bPrendido = false;
                //bTitilando = false;
                bLuzPrendida = false;
                //handler.removeCallbacks(runnable);
                SetLuz(bLuzPrendida);
            }
            int imagen = (bPrendido)? R.drawable.prendido : R.drawable.apagado;
            imgbtnInterruptor.setImageResource(imagen);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    void SetLuz(boolean prendido) {
        ReproducirSonido();
        int imagen = (prendido)? R.drawable.lampara_prendida : R.drawable.lampara_apagada;
        imgLamparita.setImageResource(imagen);
        if (bCamaraDisponible) {
            Context context = getApplicationContext();
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, prendido);
            } catch (CameraAccessException e) {
                Log.d("Error", "No hay acceso a la camara");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void establecerTitilado() {
        runnable = new Runnable(){
            public void run() {
                if (bPrendido && bTitilando) {
                    bLuzPrendida = !bLuzPrendida;
                    SetLuz(bLuzPrendida);
                }
                //handler.postAtTime(this, System.currentTimeMillis()+iIntervalo*1000);
                //handler.removeCallbacks(runnable);
                handler.postDelayed(this, iIntervalo*1000);
            }
        };
        //handler.removeCallbacks(runnable);
        //handler.postAtTime(runnable, System.currentTimeMillis()+iIntervalo*1000);
        handler.postDelayed(runnable, iIntervalo*1000);
    }

    private void ReproducirSonido() {
        sonidito.play(iSoundId, 1, 1, 1, 0, 1);
    }

    CompoundButton.OnCheckedChangeListener chkparpadeo_changed = new CompoundButton.OnCheckedChangeListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                AlertDialog mensajito = new AlertDialog.Builder(context)
                        .setTitle("Confirmar check")
                        .setMessage("Estas seguro que queres checkear este coso?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            public void onClick(DialogInterface dialog, int which) {
                                bTitilando = true;
                                //if (bPrendido) {
                                    //establecerTitilado();
                                //}
                                npkIntervalo.setEnabled(true);
                                SharedPreferences.Editor editor = preferencias.edit();
                                editor.putBoolean("CheckboxTitilando", true);
                                editor.commit();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                chkParpadeo.setChecked(false);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .create();
                mensajito.show();
            } else {
                //handler.removeCallbacks(runnable);
                bTitilando = false;
                if (bPrendido) {
                    SetLuz(true);
                }
                npkIntervalo.setEnabled(false);
                SharedPreferences.Editor editor = preferencias.edit();
                editor.putBoolean("CheckboxTitilando", false);
                editor.commit();
            }
        }
    };
}
