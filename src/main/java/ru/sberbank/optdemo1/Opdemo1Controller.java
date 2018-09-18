package ru.sberbank.optdemo1;

//import com.sun.org.apache.xpath.internal.operations.Quo;
//import com.sun.tools.corba.se.idl.InterfaceGen;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
public class Opdemo1Controller {

    private static final String URL = "http://export.rbc.ru/free/selt.0/free.fcgi?period=DAILY&tickers=USD000000TOD&separator=TAB&data_format=BROWSER";

    private static List<Quote> quotesCache = new ArrayList<>();

    @RequestMapping("/quotesCache")
    public static  @ResponseBody
    List<Quote> quotes(@RequestParam("days") int days) throws ExecutionException, InterruptedException, ParseException {
        if (quotesCache.isEmpty()) {
            AsyncHttpClient client = AsyncHttpClientFactory.create(new AsyncHttpClientFactory.AsyncHttpClientConfig());
            Response response = client.prepareGet(URL + "&lastdays=1000").execute().get();

            String body = response.getResponseBody();
            String[] lines = body.split("\n");

            Map<String, Double> maxMap = new HashMap<>();

            for (int i = 0; i < lines.length; i++) {
                String[] line = lines[i].split("\t");
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(line[1]);
                String year = line[1].split("-")[0];
                String month = line[1].split("-")[1];
                String monthYear = year + month;
                Double high = Double.parseDouble(line[3]);

                Double maxYear = maxMap.get(year);

                if (maxYear == null || maxYear < high) {
                    maxMap.put(year, high);
                    if (maxYear != null) {
                        List<Quote> newQuotes = new ArrayList<>();
                        for (Quote oldQuote : quotesCache) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(oldQuote.getDate());
                            int oldYear = cal.get(Calendar.YEAR);
                            if (oldYear == Integer.parseInt(year)) {
                                if (oldQuote.getMaxInYear() < high) {
                                    Quote newQuote = oldQuote.setMaxInYear(high);
                                    newQuotes.add(newQuote);
                                } else {
                                    newQuotes.add(oldQuote);
                                }
                            }
                        }
                        quotesCache.clear();
                        quotesCache.addAll(newQuotes);
                    }
                }

                Double maxMonth = maxMap.get(monthYear);
                if (maxMonth == null || maxMonth < high) {
                    maxMap.put(monthYear, high);
                    if (maxMonth != null) {
                        List<Quote> newQuotes = new ArrayList<>();
                        for (Quote oldQuote : quotesCache) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(oldQuote.getDate());
                            int oldYear = cal.get(Calendar.YEAR);
                            int oldMonth = cal.get(Calendar.MONTH);
                            if (oldYear == Integer.parseInt(year) && oldMonth == Integer.parseInt(month)) {
                                if (oldQuote.getMaxInMonth() < high) {
                                    Quote newQuote = oldQuote.setMaxInMonth(high);
                                    quotesCache.remove(oldQuote);
                                    quotesCache.add(newQuote);
                                }
                            }
                        }
                    }
                }

                Quote quote = new Quote(line[0],
                        new SimpleDateFormat("yyyy-MM-dd").parse(line[1]),
                        Double.parseDouble(line[2]),
                        Double.parseDouble(line[3]),
                        Double.parseDouble(line[4]),
                        Double.parseDouble(line[5]),
                        Long.parseLong(line[6]),
                        Double.parseDouble(line[7]));
                quote = quote.setMaxInMonth(maxMap.get(monthYear));
                quote = quote.setMaxInYear(maxMap.get(year));

                quotesCache.add(quote);
            }

        }

        if (quotesCache.size() < days) {
            return quotesCache.subList(0, quotesCache.size());
        } else {
            return quotesCache.subList(quotesCache.size() - days, quotesCache.size());
        }
    }
}

