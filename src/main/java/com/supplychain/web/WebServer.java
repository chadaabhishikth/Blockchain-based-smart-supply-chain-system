package com.supplychain.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.supplychain.crypto.HashFunction;
import com.supplychain.crypto.Sha256Hasher;
import com.supplychain.crypto.TransactionSerializer;
import com.supplychain.domain.dto.SupplyChainSummary;
import com.supplychain.domain.dto.TransactionMerkleProof;
import com.supplychain.domain.dto.VerificationResult;
import com.supplychain.domain.merkle.MerkleProofElement;
import com.supplychain.domain.model.Block;
import com.supplychain.domain.model.Product;
import com.supplychain.domain.model.ProductHistoryEntry;
import com.supplychain.domain.model.SupplyChainStage;
import com.supplychain.domain.model.Transaction;
import com.supplychain.domain.service.SupplyChainService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Layer — Zero-Dependency Embedded HTTP Server
 * ==================================================
 *
 * Runs a pure Java lightweight HTTP server (`com.sun.net.httpserver.HttpServer`)
 * providing a modern REST API and hosting the presentation dashboard.
 *
 * No Tomcat, Spring Boot, Node.js, or external JARs required.
 */
public class WebServer {

    private final int port;
    private final SupplyChainService service;
    private final HashFunction hasher = new Sha256Hasher();
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
        this.service = new SupplyChainService();
        initializeDemoData();
    }

    public WebServer(int port, SupplyChainService service) {
        this.port = port;
        this.service = service;
    }

    /**
     * Pre-populate realistic supply chain journey so presentation is demo-ready immediately.
     */
    private void initializeDemoData() {
        service.registerManufacturer("AUTHENTIC-FACTORY-A");
        service.registerManufacturer("AUTHENTIC-FACTORY-B");

        // Step 1: Manufacture
        Map<String, Object> meta1 = new LinkedHashMap<>();
        meta1.put("item", "Luxury Chronograph");
        service.manufactureProduct("AUTHENTIC-FACTORY-A", Collections.singletonList("PROD-001"),
                "Geneva Manufacturing Center", "BATCH-LUX-2026-01", meta1);

        Map<String, Object> meta2 = new LinkedHashMap<>();
        meta2.put("item", "Flagship Smartphone");
        service.manufactureProduct("AUTHENTIC-FACTORY-B", Collections.singletonList("PROD-002"),
                "Shenzhen Tech Plant", "BATCH-TECH-2026-09", meta2);

        // Step 2: Distribution
        service.transferOwnership("PROD-001", "AUTHENTIC-FACTORY-A", "LOGISTICS-EXPRESS-EUR",
                "Zurich Freight Hub", SupplyChainStage.DISTRIBUTION, null);

        // Step 3: Wholesaler
        service.transferOwnership("PROD-001", "LOGISTICS-EXPRESS-EUR", "GLOBAL-WHOLESALE-INC",
                "Frankfurt Central Depot", SupplyChainStage.WHOLESALING, null);

        // Step 4: Retailer
        service.transferOwnership("PROD-001", "GLOBAL-WHOLESALE-INC", "LUXURY-BOUTIQUE-PARIS",
                "Paris Champs-Elysees Store", SupplyChainStage.RETAIL, null);

        // Step 5: Consumer Sale
        service.sellToConsumer("PROD-001", "LUXURY-BOUTIQUE-PARIS", "CONSUMER-VIP-88",
                "Paris Champs-Elysees Store", 12500.00, null);
    }

    /**
     * Start the HTTP server.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Route definitions
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/blocks", new BlocksHandler());
        server.createContext("/api/verify", new VerifyHandler());
        server.createContext("/api/merkle", new MerkleHandler());
        server.createContext("/api/manufacture", new ManufactureHandler());
        server.createContext("/api/tamper", new TamperHandler());
        server.createContext("/api/restore", new RestoreHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("======================================================================");
        System.out.println("🚀 SMARTCHAIN PRESENTATION DASHBOARD STARTED");
        System.out.println("👉 Open your browser at: http://localhost:" + port);
        System.out.println("======================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public SupplyChainService getService() {
        return service;
    }

    // --- HTTP HANDLERS ---

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }
            String html = WebDashboardResource.getHtml();
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            SupplyChainSummary summary = service.getSummary();
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"totalBlocks\":").append(summary.getTotalBlocks()).append(",");
            json.append("\"totalProducts\":").append(summary.getTotalProducts()).append(",");
            json.append("\"activeProducts\":").append(summary.getActiveProducts()).append(",");
            json.append("\"soldProducts\":").append(summary.getSoldProducts()).append(",");
            json.append("\"authorizedManufacturers\":").append(summary.getRegisteredManufacturers()).append(",");
            json.append("\"isValid\":").append(summary.isBlockchainValid());
            json.append("}");

            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }

    private class BlocksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Block> chain = service.getLedger().getChain();
            boolean chainValid = service.getLedger().isValid();

            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < chain.size(); i++) {
                Block b = chain.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"index\":").append(b.getIndex()).append(",");
                json.append("\"timestamp\":\"").append(escape(b.getTimestamp())).append("\",");
                json.append("\"hash\":\"").append(escape(b.getHash())).append("\",");
                json.append("\"previousHash\":\"").append(escape(b.getPreviousHash())).append("\",");
                json.append("\"merkleRoot\":\"").append(escape(b.getMerkleRoot())).append("\",");
                json.append("\"tampered\":").append(!chainValid && i == 1).append(",");
                json.append("\"transactions\":[");
                List<Transaction> txs = b.getTransactions();
                for (int t = 0; t < txs.size(); t++) {
                    Transaction tx = txs.get(t);
                    if (t > 0) json.append(",");
                    json.append("{");
                    json.append("\"productId\":\"").append(escape(tx.getProductId())).append("\",");
                    json.append("\"sender\":\"").append(escape(tx.getSender())).append("\",");
                    json.append("\"receiver\":\"").append(escape(tx.getReceiver())).append("\",");
                    json.append("\"location\":\"").append(escape(tx.getLocation())).append("\",");
                    json.append("\"timestamp\":\"").append(escape(tx.getTimestamp())).append("\"");
                    json.append("}");
                }
                json.append("]}");
            }
            json.append("]");

            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }

    private class VerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String productId = null;
            if (query != null && query.contains("productId=")) {
                productId = query.substring(query.indexOf("productId=") + 10);
                int amp = productId.indexOf('&');
                if (amp != -1) productId = productId.substring(0, amp);
                productId = java.net.URLDecoder.decode(productId, StandardCharsets.UTF_8);
            }

            if (productId == null || productId.trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Missing productId parameter\"}", "application/json");
                return;
            }

            VerificationResult vr = service.verifyProduct(productId);
            TransactionMerkleProof proof = service.getLatestProductMerkleProof(productId);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"productId\":\"").append(escape(productId)).append("\",");
            json.append("\"authentic\":").append(vr.isAuthentic()).append(",");
            json.append("\"verified\":").append(vr.isVerified()).append(",");

            if (!vr.isAuthentic()) {
                json.append("\"reason\":\"").append(escape(vr.getReason() != null ? vr.getReason() : "UNREGISTERED_ITEM")).append("\",");
                json.append("\"recommendation\":\"").append(escape(vr.getRecommendation() != null ? vr.getRecommendation() : "Do not purchase.")).append("\"");
            } else {
                json.append("\"manufacturer\":\"").append(escape(vr.getManufacturer())).append("\",");
                json.append("\"currentOwner\":\"").append(escape(vr.getCurrentOwner())).append("\",");
                json.append("\"currentStatus\":\"").append(escape(vr.getCurrentStatus())).append("\",");
                json.append("\"recommendation\":\"").append(escape(vr.getRecommendation())).append("\",");
                json.append("\"journey\":[");
                List<ProductHistoryEntry> journey = vr.getJourney();
                if (journey != null) {
                    for (int j = 0; j < journey.size(); j++) {
                        ProductHistoryEntry entry = journey.get(j);
                        if (j > 0) json.append(",");
                        json.append("{");
                        json.append("\"blockIndex\":").append(entry.getBlockIndex()).append(",");
                        json.append("\"blockHash\":\"").append(escape(entry.getBlockHash())).append("\",");
                        json.append("\"sender\":\"").append(escape(entry.getSender())).append("\",");
                        json.append("\"receiver\":\"").append(escape(entry.getReceiver())).append("\",");
                        json.append("\"location\":\"").append(escape(entry.getLocation())).append("\",");
                        json.append("\"timestamp\":\"").append(escape(entry.getTimestamp())).append("\"");
                        json.append("}");
                    }
                }
                json.append("],");

                // Merkle Proof
                if (proof != null) {
                    json.append("\"merkleProof\":{");
                    json.append("\"transactionHash\":\"").append(escape(proof.getTransactionHash())).append("\",");
                    json.append("\"blockMerkleRoot\":\"").append(escape(proof.getBlockMerkleRoot())).append("\",");
                    json.append("\"proofVerified\":").append(service.verifyLightweightProof(proof)).append(",");
                    json.append("\"proofElements\":[");
                    List<MerkleProofElement> elements = proof.getProofElements();
                    for (int e = 0; e < elements.size(); e++) {
                        MerkleProofElement el = elements.get(e);
                        if (e > 0) json.append(",");
                        json.append("{");
                        json.append("\"side\":\"").append(el.getSide().name()).append("\",");
                        json.append("\"siblingHash\":\"").append(escape(el.getSiblingHash())).append("\"");
                        json.append("}");
                    }
                    json.append("]}");
                } else {
                    json.append("\"merkleProof\":null");
                }
            }
            json.append("}");

            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }

    private class MerkleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            int blockIndex = 0;
            if (query != null && query.contains("blockIndex=")) {
                try {
                    String val = query.substring(query.indexOf("blockIndex=") + 11);
                    int amp = val.indexOf('&');
                    if (amp != -1) val = val.substring(0, amp);
                    blockIndex = Integer.parseInt(val);
                } catch (Exception ignored) {
                }
            }

            Block block = service.getLedger().getBlock(blockIndex);
            if (block == null) {
                sendResponse(exchange, 404, "{\"error\":\"Block not found\"}", "application/json");
                return;
            }

            // Build levels of the Merkle Tree bottom-up
            List<String> leaves = new ArrayList<>();
            for (Transaction tx : block.getTransactions()) {
                leaves.add(TransactionSerializer.hashOf(tx, hasher));
            }

            List<List<String>> levels = new ArrayList<>();
            if (!leaves.isEmpty()) {
                List<String> currentLevel = new ArrayList<>(leaves);
                levels.add(new ArrayList<>(currentLevel));

                while (currentLevel.size() > 1) {
                    List<String> nextLevel = new ArrayList<>();
                    for (int i = 0; i < currentLevel.size(); i += 2) {
                        String left = currentLevel.get(i);
                        String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                        nextLevel.add(hasher.hash(left + right));
                    }
                    currentLevel = nextLevel;
                    levels.add(new ArrayList<>(currentLevel));
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"blockIndex\":").append(block.getIndex()).append(",");
            json.append("\"root\":\"").append(escape(block.getMerkleRoot())).append("\",");
            json.append("\"levels\":[");
            for (int l = 0; l < levels.size(); l++) {
                if (l > 0) json.append(",");
                json.append("[");
                List<String> lvl = levels.get(l);
                for (int h = 0; h < lvl.size(); h++) {
                    if (h > 0) json.append(",");
                    json.append("\"").append(escape(lvl.get(h))).append("\"");
                }
                json.append("]");
            }
            json.append("]}");

            sendResponse(exchange, 200, json.toString(), "application/json");
        }
    }

    private class ManufactureHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseSimpleJson(body);

            String productId = params.get("productId");
            String factory = params.getOrDefault("factory", "AUTHENTIC-FACTORY-A");
            String location = params.getOrDefault("location", "Standard Facility");
            String batch = params.getOrDefault("batch", "BATCH-DEFAULT");

            if (productId == null || productId.trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"Missing productId\"}", "application/json");
                return;
            }

            service.registerManufacturer(factory);
            try {
                var res = service.manufactureProduct(factory, Collections.singletonList(productId), location, batch, null);
                sendResponse(exchange, 200, "{\"success\":true,\"blockIndex\":" + res.getBlockIndex() + "}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class TamperHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (service.getLedger().getChainLength() > 1) {
                service.simulateTampering(1, 0, "ILLEGAL BLACK MARKET DEPOT");
                sendResponse(exchange, 200, "{\"success\":true,\"tamperedBlock\":1}", "application/json");
            } else {
                sendResponse(exchange, 400, "{\"error\":\"Not enough blocks to tamper\"}", "application/json");
            }
        }
    }

    private class RestoreHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean restored = service.restoreChain();
            sendResponse(exchange, 200, "{\"success\":" + restored + "}", "application/json");
        }
    }

    // --- UTILITIES ---

    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String clean = json.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);

        String[] pairs = clean.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception ignored) {
            }
        }
        new WebServer(port).start();
    }
}
