package com.aurd;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

    public static void main(String[] args) {
	// write your code here
        try {

            for(int x=1;x<5;x++) {
                Document doc = Jsoup.connect("https://www.propertyfinder.qa/en/rent/compounds-for-rent.html?page="+x).get();
                //  System.out.println(doc);
                // doc.
                Elements cardlist = doc.select("div[data-qs=cardlist]");

                Elements cards = cardlist.select("div[class=card__content]");

                //System.out.println(cards.first());

                cards.forEach(element -> {
                    String name = element.select("h2[class=\"card__title card__title-link\"]").text();
                    String price = element.select("span[class=\"card__price-value\"]").text();
                    String location = element.select("span[class=\"card__location-text\"]").text();
                    System.out.println(name);
                    System.out.println(price);
                    System.out.println(location);

                });
//           for(int i=0;i<cards.size();i++)
//           {
//               Element element=cards.get(i);
//               System.out.println(cards.get(i));
//           }

                // System.out.println(cardlist.select("div[class=card__content]").first());
//            for (Element link : card) {
//                System.out.println(link);
//            }

                Thread.sleep(2000);
            }
        }catch (Exception e)
        {
            System.out.println(e);
        }
    }
}
