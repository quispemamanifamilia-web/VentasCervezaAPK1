package com.ventas.cerveza;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.print.PrintManager;
import android.content.Context;
import android.util.Base64;
import android.widget.Toast;
import java.io.OutputStream;

public class MainActivity extends Activity {
  WebView web; ValueCallback<Uri[]> chooser; String pendingName="ventas.json",pendingType="application/json"; byte[] pendingBytes;
  static final int PICK_FILE=41, SAVE_FILE=42;
  @Override public void onCreate(Bundle b){super.onCreate(b);
    web=new WebView(this); setContentView(web);
    WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);
    web.setWebViewClient(new WebViewClient());
    web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> cb,FileChooserParams params){chooser=cb;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_FILE);return true;}});
    web.addJavascriptInterface(new Object(){
      @JavascriptInterface public void printPage(){runOnUiThread(()->{PrintManager pm=(PrintManager)getSystemService(Context.PRINT_SERVICE);pm.print("Reporte ventas",web.createPrintDocumentAdapter("Ventas cerveza"),null);});}
      @JavascriptInterface public void saveFile(String name,String content,String type){runOnUiThread(()->{pendingName=name;pendingType=type;pendingBytes=content.getBytes(java.nio.charset.StandardCharsets.UTF_8);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,SAVE_FILE);});}
    },"Android");
    web.loadUrl("file:///android_asset/index.html");
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
    if(request==PICK_FILE){if(chooser!=null){chooser.onReceiveValue(result==RESULT_OK&&data!=null?new Uri[]{data.getData()}:null);chooser=null;}}
    if(request==SAVE_FILE&&result==RESULT_OK&&data!=null&&pendingBytes!=null){try(OutputStream o=getContentResolver().openOutputStream(data.getData())){o.write(pendingBytes);Toast.makeText(this,"Archivo guardado",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"No se pudo guardar",Toast.LENGTH_LONG).show();}}
  }
  @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
