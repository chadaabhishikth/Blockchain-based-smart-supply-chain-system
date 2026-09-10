package com.supplychain.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Web Layer — Embedded Dashboard HTML/CSS/JS Resource
 * ====================================================
 *
 * Provides the single-page application for the presentation demo.
 * Colors customized according to the design specification:
 *   - Dark Slate / Charcoal: #524646
 *   - Muted Olive / Gray:     #A8A492
 *   - Warm Cream Background:  #FCF2E5
 *   - Vibrant Terracotta:     #EC5B38
 */
public final class WebDashboardResource {

    private static String cachedHtml = null;

    private WebDashboardResource() {
    }

    /**
     * Load the HTML dashboard content.
     */
    public static synchronized String getHtml() {
        if (cachedHtml != null) {
            return cachedHtml;
        }

        // Try loading from classpath first
        try (InputStream in = WebDashboardResource.class.getResourceAsStream("/web/index.html")) {
            if (in != null) {
                cachedHtml = readStream(in);
                return cachedHtml;
            }
        } catch (Exception ignored) {
        }

        // Fallback: load directly from file system path
        File file = new File("src/main/resources/web/index.html");
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                cachedHtml = readStream(in);
                return cachedHtml;
            } catch (Exception ignored) {
            }
        }

        return "<html><body><h1>SmartChain Dashboard</h1><p>index.html not found.</p></body></html>";
    }

    private static String readStream(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}