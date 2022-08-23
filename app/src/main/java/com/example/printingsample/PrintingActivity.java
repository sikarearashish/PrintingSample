package com.example.printingsample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Printer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class PrintingActivity extends AppCompatActivity implements BTName.Senddata {

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    TextView lblPrinterName;
    EditText textBox;
    Dialog dialog;
    BTName btNameadapter;
    BTName.Senddata senddata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printing);
        senddata = this;
        // Create object of controls
        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        Button btnPrint = (Button) findViewById(R.id.btnPrint);

        textBox = (EditText) findViewById(R.id.txtText);

        lblPrinterName = (TextView) findViewById(R.id.lblPrinterName);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (ContextCompat.checkSelfPermission(PrintingActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED
                            &&
                            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        ActivityCompat.requestPermissions(PrintingActivity.this,
                                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                                1);
                        return;
                    } else {
                        FindBluetoothDevice();
                        //openBluetoothPrinter();
                    }


                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    disconnectBT();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    printData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

    }

    ArrayList<BluetoothDevice> names = new ArrayList<>();
    Set<BluetoothDevice> pairedDevice;

    void FindBluetoothDevice() {

        try {

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                lblPrinterName.setText("No Bluetooth Adapter found");
            }
            if (bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 0);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            pairedDevice = bluetoothAdapter.getBondedDevices();
            names.clear();
            names.addAll(pairedDevice);
            if (pairedDevice.size() > 0) {
                for (BluetoothDevice pairedDev : pairedDevice) {

                    // My Bluetoth printer name is BTP_F09F1A
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }
                    if (pairedDev.getName().equals("BTP_F09F1A")) {
                        bluetoothDevice = pairedDev;
                        lblPrinterName.setText("Bluetooth Printer Attached: " + pairedDev.getName());
                        break;
                    }
                }
            }

            dialog = new Dialog(PrintingActivity.this);
            dialog.setContentView(R.layout.bt_name);
            RecyclerView recy_name = dialog.findViewById(R.id.recy_name);
            recy_name.setHasFixedSize(true);
            recy_name.setLayoutManager(new LinearLayoutManager(this));

            btNameadapter = new BTName(names, senddata);
            recy_name.setAdapter(btNameadapter);
            dialog.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    // Open Bluetooth Printer

    void openBluetoothPrinter() throws IOException {
        try {

            //Standard uuid from string //
            UUID uuidSting = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuidSting);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            beginListenData();

        } catch (Exception ex) {

        }
    }

    void beginListenData() {
        try {

            final Handler handler = new Handler();
            final byte delimiter = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int byteAvailable = inputStream.available();
                            if (byteAvailable > 0) {
                                byte[] packetByte = new byte[byteAvailable];
                                inputStream.read(packetByte);

                                for (int i = 0; i < byteAvailable; i++) {
                                    byte b = packetByte[i];
                                    if (b == delimiter) {
                                        byte[] encodedByte = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedByte, 0,
                                                encodedByte.length
                                        );
                                        final String data = new String(encodedByte, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                lblPrinterName.setText(data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            stopWorker = true;
                        }
                    }

                }
            });

            thread.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
   String cnno="1027765095069056";
   String date="20.10.2202";
    String inst="DOOR";
    String mode="AIR";
    String Consigner="1027765095069056";
    String Consignee="3AACPgdfgdf";
    String Consignercom="Mahile Anand Filter";
    String Consigneecom="Mahile Anand Filter";
    String from="Mahile Anand Filter Delhi";
    String to="Mahile Anand Filter Mumbai";
    String con_gst="5446546546";
    String conee_gst="546546545FDGFDFG";
    String p_mode="To Bill";
    String Freight="34555";
    String billing_party="36565ZXS4555";
    String compname="TATA Motors";
    String billing_location="TATA Motors";
    String amount="TATA Motors";
    String chequeno="";
    String mrdate="";
    String nnvnum="9204703925";
    String date1="23/12/22";
    String memo ="Ap";
    String pkgtype ="CRA";
    String pkgQTY ="10";
    String act ="10";
    String itemsescription ="including FOV Dummerge ,Delvery Chrges , All Charges Apply ";
    String net ="10";
    String gross ="10";

    // Printing Text to Bluetooth Printer //
    void printData() throws IOException {
        try {
            cnno=textBox.getText().toString();
            String msg ="";
            msg += "\n"+"CN NO: "+GetSpaces(cnno,16)+ " CN DATE "+date+"\n"  //23 break
                   //total is 48
           +"Delevery inst. "+GetSpaces(inst,8) +                 " Booking Mode. "+mode+"\n\n"
            +"Consigner              " +                               " Consignee "+"\n"
            +GetSpaces(Consigner,23)+                                 " "+Consignee+"\n"
            +GetSpaces(Consignercom,23)                    +" "+GetSpaces(Consigneecom,23)+"\n"
            +  "FROM                   " +                  " TO "+"\n"
            +GetSpaces(from,23)+                       " "+GetSpaces(to,23)+"\n"+
            GetSpaceswith2lines(from,23)+                       " "+GetSpaceswith2lines(to,23)+"\n"+
                    GetSpaces(con_gst,23)+              " "+GetSpaces(conee_gst,23)+"\n"
                    +"------------------------------------------------\n\n"+
                    "     Payment Mode      " +                               "    Freight Details    "+"\n"+
                    "Mode "+GetSpaces(p_mode,18)+                      " Freight          "+Freight+"\n"+
                    "Billing Party          "+                              " ST Charge        "+Freight+"\n"+
                    GetSpaces(billing_party,23)+                      " Collection Charge "+Freight+"\n"+
                    GetSpaces(compname,23)+                           " Fov Charges       "+Freight+"\n"+
                    "                       "+                             " Humali Charges    "+Freight+"\n"+
                    "                       "+                             " Demmurage Charges "+Freight+"\n"+
                    "Billing Location\n"+
                    billing_location+"\n\n"+
                    "                       "+                      " Gross Total      "+Freight+"\n"+
                    "Amount    "+GetSpaces( amount,13)+        " SGST Charge      "+Freight+"\n"+
                    "MR/Cheque No:"+GetSpaces( chequeno,10)  + " CGST Total       "+Freight+"\n"+
                    "MR Date: "+GetSpaces(mrdate,14)+          " IGST Total       "+Freight+"\n"+
                    "                       "+                      " Total            "+Freight+"\n\n"
                    +"--------------Invoice Details-------------\n\n"
                    +"------------------------------------------------\n"+
                    "Inv.No/    |"+"PKG/|"+"Inv.Value/ |"+"EwayNo|"+"Item     \n"+
                    "Date       |"+"Wt  |"+"Gross.Value|"+"Expiry|"+"Description\n"+
                    "------------------------------------------------\n";
            for(int i=0;i<4;i++){
                msg=msg+GetSpaces(nnvnum,12)+GetSpaces(memo,5)+       GetSpaces(pkgtype,11)+  GetSpaces(pkgQTY,7)+    GetSpaces(itemsescription,9)+"\n"
                        +GetSpaces(date1,12)+GetSpaces("",5)+GetSpaces("",11)+GetSpaces("",7)+GetSpaceswith2lines(itemsescription,9)+"\n"
                +"------------------------------------------------\n";
            }

            msg=msg +"--------------Invoice Details-------------\n\n\n\n";

            /*
          */

             outputStream.write(msg.getBytes());
            lblPrinterName.setText("Printing Text...");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Disconnect Printer //
    void disconnectBT() throws IOException {
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblPrinterName.setText("Printer Disconnected.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    int spinner_item_location = -1;

    @Override
    public void sendPos(int i) {
        spinner_item_location = i;
        MyTask myTask = new MyTask();
        myTask.execute();

    }

    private void createBond(BluetoothDevice device) throws Exception {

        try {
            Class<?> cl = Class.forName("android.bluetooth.BluetoothDevice");
            Class<?>[] par = {};
            Method method = cl.getMethod("createBond", par);
            method.invoke(device);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    boolean isConnected() {
        return bluetoothSocket != null;
    }

    void disconnect() {
        if (bluetoothSocket == null) {
            // showToast("Socket is not connected");
        }

        try {
            bluetoothSocket.close();
            bluetoothSocket = null;
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    ProgressDialog progressDialog;

    public class MyTask extends AsyncTask {
        @Override
        protected String doInBackground(Object[] objects) {
            String res = null;
            if (pairedDevice == null || pairedDevice.size() == 0) {
                return null;
            }

            bluetoothDevice = names.get(spinner_item_location);
            if (ActivityCompat.checkSelfPermission(PrintingActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return null ;
            }
            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                try {
                    createBond(bluetoothDevice);
                } catch (Exception e) {
                    //showToast("Failed to pair device");
                    return null;
                }
            }
            try {

                if (!isConnected()) {
                    openBluetoothPrinter();
                    res = "connected";
                } else {
                    disconnect();
                    res = "disconnected";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(PrintingActivity.this);
            progressDialog.setTitle("progress");
            progressDialog.setMessage("connecting...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Object o) {
            Log.e("onPostExecute", "working");
            if (o.toString().equals("connected")) {
             //   progressDialog.dismiss();
                lblPrinterName.setText(names.get(spinner_item_location).getName().toString()+" Connected");


            }
            if (o.toString().equals("disconnected")) {

                lblPrinterName.setText(names.get(spinner_item_location).getName().toString()+" Disconnected");
            }
            if(dialog.isShowing())
                dialog.dismiss();
            progressDialog.dismiss();

        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
        }
    }


    public String GetSpaces(String mystring ,int total){
        String s=mystring;
        if(total>=mystring.length()){
            int mycount=total-mystring.length();
            for(int i=0;i<mycount;i++){
                s=s+" ";
            }
        }
        else{
            s=s.substring(0,total);
        }
       return s;
    }


    public String GetSpaceswith2lines(String mystring ,int total){
        String s="";
        if(total<mystring.length()){
           s=mystring.substring(total);
           s=GetSpaces(s,total);
        }
        else{
            s=GetSpaces(s,total);
        }
       return s;
    }

}