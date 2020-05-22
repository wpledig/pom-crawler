package com.ledig;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class CrawlerApp {

    /*
     * Arguments:
     *  [0] - startURL - URL of location to search
     *  [1] - outputDir - Directory to save POM files to
     *  [2] - delay - Number of milliseconds to wait between any HTTP calls
     *  [3] - maxCount (OPTIONAL) - Maximum number of POM files to download
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 3) {
            System.out.println("Invalid number of arguments inputted!");
            return;
        }
        String startURL = args[0]; // URL to start search at
        String outputDir = args[1]; // Directory to output results to
        int delay = Integer.parseInt(args[2]); // Number of milliseconds to delay calls
        int maxCount = -1; // Max number of POM files to download
        if (args.length > 3) {
            maxCount = Integer.parseInt(args[3]);
        }

        //Queue<String> urlQ = new LinkedList<>();
        Stack<String> urlStack = new Stack<>();
        urlStack.push(startURL);
        int pomsCollected = 0;
        Date lastReq = new Date();

        while (!urlStack.isEmpty()) {
            String curURL = urlStack.pop();
            System.out.println();
            System.out.println("Searching URL: " + curURL);
            System.out.println("----------------------------");

            // Get HTML + Parse
            Thread.sleep(lastReq.getTime() + delay - new Date().getTime());
            Document doc = Jsoup.connect(curURL).get();
            lastReq = new Date();
            System.out.println("Found page: " + doc.title());

            Elements links = doc.select("a");

            for (Element link : links) {
                String linkLoc = link.attr("href");
                System.out.println("Link: " + linkLoc);

                // Check if links end in ".pom"
                if (linkLoc.endsWith(".pom")) {
                    String pomLoc = formatURL(curURL, linkLoc);
                    String filename = outputDir + "/" + getFilename(pomLoc);

                    System.out.println("*** Found pom at: " + pomLoc);
                    Thread.sleep(lastReq.getTime() + delay - new Date().getTime());
                    FileUtils.copyURLToFile(new URL(pomLoc), new File(filename));
                    lastReq = new Date();
                    System.out.println("*** Copied pom to: " + filename);

                    pomsCollected++;
                    if (maxCount > 0 && pomsCollected >= maxCount) {
                        System.out.println("-------------------------------");
                        System.out.println("Finished running!");
                        System.out.println("Collected " + pomsCollected + " pom files.");
                        return;
                    }
                }

                // Check if links start with same URL + end in '/'
                if (linkLoc.endsWith("/") && !(linkLoc.equals("./") || linkLoc.equals("../"))) {
                    if (linkLoc.startsWith(curURL)) {
                        urlStack.push(linkLoc);
                    } else if (!(linkLoc.startsWith("http://") || linkLoc.startsWith("https://"))) {
                        // TODO: probably need to make this more robust, but likely works for now
                        urlStack.push(curURL + linkLoc);
                    }
                }
            }
        }
        System.out.println("-------------------------------");
        System.out.println("Finished running!");
        System.out.println("Collected " + pomsCollected + " pom files.");
    }

    private static String getFilename(String url) {
        return url.substring(url.lastIndexOf('/'));
//        return url.substring(url.indexOf('/'));
    }

    private static String formatURL(String parent, String child) {
        if (child.startsWith(parent)) {
            return child;
        } else {
            return parent + child;
        }
    }
}
