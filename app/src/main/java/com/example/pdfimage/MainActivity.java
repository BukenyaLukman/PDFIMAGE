package com.example.pdfimage;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.pdfimage.Models.SuperHeroModels;
import com.example.pdfimage.Utils.PDFDocumentAdapter;
import com.example.pdfimage.Utils.PDFUtils;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private Button CreatePDFBtn;
    private static final String FILE_PRINT = "test_results.pdf";
    private AlertDialog dialog;

    List<SuperHeroModels> superHeroModelsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CreatePDFBtn = findViewById(R.id.create_pdf_btn);
        dialog = new AlertDialog.Builder(this).setCancelable(false).setMessage("Please wait").create();

        addSuperHeroes();

        CreatePDFBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dexter.withContext(MainActivity.this)
                        .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                createPDFFile(new StringBuilder(getApppPath())
                                        .append(FILE_PRINT).toString());
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                Toast.makeText(MainActivity.this, ""+ permissionDeniedResponse.getPermissionName() + "need enable", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                            }
                        }).check();
            }
        });


    }

    private void createPDFFile(String path) {
        if(new File(path).exists())
            new File(path).delete();
        try{
            Document document = new Document();
            //Save
            PdfWriter.getInstance(document, new FileOutputStream(path));
            document.open();

            //setting
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            document.addAuthor("Bukenya Lukman");
            document.addCreator("Bukenya Lukman");

            //Font Settings
            BaseColor colorAccent = new BaseColor(0,153,204,255);
            float fontSize = 20.0f;
            //Custom Font
            BaseFont fontName = BaseFont.createFont("assets/fonts/brandon_medium.otf","UTF-8",BaseFont.EMBEDDED);

            //Create Title of document
            Font titleFont = new Font(fontName,36.0f, Font.NORMAL, BaseColor.BLACK);
            PDFUtils.addNewItem(document, "NEXT MEDIA",Element.ALIGN_CENTER,titleFont);

            //Add More information
            Font textFont = new Font(fontName,fontSize,Font.NORMAL,colorAccent);
            PDFUtils.addNewItem(document, "Document By: ", Element.ALIGN_LEFT,titleFont);
            PDFUtils.addNewItem(document, "Bukenya Lukman: ", Element.ALIGN_LEFT,titleFont);

            PDFUtils.addLineSeperator(document);

            //Add detail
            PDFUtils.addLineSeperator(document);
            PDFUtils.addNewItem(document, "DETAIL ", Element.ALIGN_CENTER,titleFont);
            PDFUtils.addLineSeperator(document);

            //Use RxJava, fetch image From URL and add to PDF
            Observable.fromIterable(superHeroModelsList)
                    .flatMap(model -> getBitmapFromUrl(this, model, document))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(model ->{
                        PDFUtils.addNewItemWithLeftAndRight(document,model.getName(), "",titleFont,textFont);

                        PDFUtils.addLineSeperator(document);

                        PDFUtils.addNewItem(document, model.getDescription(), Element.ALIGN_LEFT,textFont);

                        PDFUtils.addLineSeperator(document);


                    },throwable -> {
                        dialog.dismiss();
                        Toast.makeText(this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }, ()->{
                        //On Complete
                        //When complete, close Document
                        document.close();
                        dialog.dismiss();
                        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();

                        PrintPDF();
                    });


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            dialog.dismiss();
        }
    }

    private void PrintPDF() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        try{
            PrintDocumentAdapter printDocumentAdapter = new PDFDocumentAdapter(this, new StringBuilder(getApppPath())
                    .append(FILE_PRINT).toString(), FILE_PRINT);
            printManager.print("Document",printDocumentAdapter,new PrintAttributes.Builder().build());
            
        }catch(Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Observable<SuperHeroModels> getBitmapFromUrl(Context context, SuperHeroModels model, Document document) {

        return Observable.fromCallable(() ->{
            Bitmap bitmap = Glide.with(context)
                    .asBitmap()
                    .load(model.getImage())
                    .submit().get();
            Image image = Image.getInstance(bitmapToByteArray(bitmap));
            image.scaleAbsolute(100,100);
            document.add(image);
            return model;
        });
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100,stream);
        return stream.toByteArray();
    }

    private String getApppPath() {
        File dir = new File(android.os.Environment.getExternalStorageDirectory()+
                File.separator
        + getResources().getString(R.string.app_name)
        + File.separator);
        if(!dir.exists())
            dir.mkdir();
        return dir.getPath() + File.separator;

    }


    private void addSuperHeroes() {
        SuperHeroModels superHeroModels = new SuperHeroModels("Next Media",
                "https://avatarfiles.alphacoders.com/131/131715.jpg",
                "Next Media is a Leading BroadCasting Comapnay in Uganda");
        superHeroModelsList.add(superHeroModels);
        superHeroModels = new SuperHeroModels("Next Media", "https://cdn.tgdd.vn/Files/2019/04/29/1163886/avengersendgame_800x450.jpg",
                "Next Media is based in Kampala, Uganda and is regarded the political command center");
        superHeroModelsList.add(superHeroModels);


    }

}