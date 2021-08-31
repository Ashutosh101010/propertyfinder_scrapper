package com.aurd;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        ConnectionString connectionString = new ConnectionString("mongodb://ashutosh:Ashutosh96@94.237.121.70:27017/test?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&ssl=false");
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("RevueStage");

        MongoCollection collection=database.getCollection("Compounds");


        final String s3Endpoint = "https://s3.wasabisys.com";
        final String region = "us-east-1";

        AWSCredentialsProvider credentials =
                new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials("RO3PDA6CRJSQC4JH65QH", "dtB4aV9tharNtWaW2eaZMK08zCzqHlleBMvlmRof"));

       AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new

                        AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                .withCredentials(credentials)
                .build();


        try {

            for(int x=1;x<5;x++) {
                Document doc = Jsoup.connect("https://www.propertyfinder.qa/en/search?c=2&fu=0&ob=mr&page="+x).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.164 Safari/537.36")
                        .referrer("http://www.google.com")
                        .get();


//                  System.out.println(doc);
                // doc.
                Elements cardlist = doc.select("div[data-qs=cardlist]");

                Elements cards = cardlist.select("div[class=card-list__item]");

                //System.out.println(cards.first());

                JSONArray array=new JSONArray();


                cards.forEach(element -> {
//                    System.out.println(element);
                    try {
                    String name = element.select("h2[class=\"card__title card__title-link\"]").text();
                    String price = element.select("span[class=\"card__price-value\"]").text();
                    String location = element.select("span[class=\"card__location-text\"]").text();
                    String  link = element.select("a").first().attr("href");

                    Document propertyDoc = Jsoup.connect("https://www.propertyfinder.qa"+link).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.164 Safari/537.36")
                                .referrer("http://www.google.com")
                                .get();



                        JSONArray images=new JSONArray();
                        Element gallery=propertyDoc.select("div[class=property-page__gallery]").first();
                      images.put(gallery.select("picture[class=property-page__gallery-big-image]").select("source").first().attr("srcset"));
                      images.put(gallery.select("picture[class=property-page__gallery-small-image]").select("source").select("source").first().attr("srcset"));
                      images.put(gallery.select("picture[class=property-page__gallery-small-image]").select("source").select("source").get(1).attr("srcset"));
//                        System.out.println(gallery);
                      String propertyType=  propertyDoc.selectFirst("div[class=property-facts__list]").selectFirst("div[class=text text--bold property-facts__content]").text();

                      String json=propertyDoc.select("script[type=application/ld+json]").last().data();
                        JSONArray script=new JSONArray(json);
                        String description=script.getJSONObject(0).getJSONObject("mainEntity").getString("description");


                        Double latitude=script.getJSONObject(1).getJSONObject("geo").getDouble("latitude");
                        Double longitude=script.getJSONObject(1).getJSONObject("geo").getDouble("longitude");


                        ArrayList<String> amenities=new ArrayList<>();
                        JSONArray amenityJsonList=script.getJSONObject(1).getJSONArray("amenityFeature");
                        amenityJsonList.forEach(o -> {
                           JSONObject j= ((JSONObject) o);
                           amenities.add(j.getString("name"));
                        });

                        JSONObject output=new JSONObject();
                        output.put("amenities",amenities);
//                        output.put("images",images);
                        output.put("description",description.trim());
                        JSONObject o=new JSONObject();
                        o.put("coordinates",new JSONArray("["+latitude+","+longitude+"]"));
                        o.put("type","Point");
                        output.put("position",o);
//                       output.put("coordinates",new JSONArray("["+latitude+","+longitude+"]"));
                       output.put("category",propertyType);
                       name=location.split(",")[0];
                       output.put("compoundname",name.trim());
                       output.put("address",location);
//
//                        output.put("location",location);
                        array.put(output);
//
                        org.bson.Document insert= org.bson.Document.parse(output.toString());
                        insert.append("facility",0.00);
                        insert.append("location",0.00);
                        insert.append("management",0.00);
                        insert.append("value",0.00);
                        insert.append("design",0.00);
                        insert.append("rating",0.00);

                        org.bson.Document find=new org.bson.Document();
                        find.append("address",location.trim());
                        find.append("compoundname",name.trim());

                      boolean exists=  collection.find(find).cursor().hasNext();
                      if(!exists)
                      {

                          JSONArray image=new JSONArray();
                          images.forEach(o1 -> {
                              try {
                                  Thread.sleep(1);
                                  String ImageId = String.valueOf(System.currentTimeMillis());
                                  URL url = new URL((String) o1);
                                  InputStream in = new BufferedInputStream(url.openStream());
                                  s3.putObject("revue", ImageId, in, new ObjectMetadata());

                                  image.put(ImageId);


                              }catch (Exception e)
                              {
                                  e.printStackTrace();
                              }
                          });
                         insert.append("images",image) ;
                         collection.insertOne(insert);
                      }

                        Thread.sleep(2000);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                });

                System.out.println(array.toString(4));
                Thread.sleep(2000);
            }
        }catch (Exception e)
        {
            System.out.println(e);
        }
    }


}
