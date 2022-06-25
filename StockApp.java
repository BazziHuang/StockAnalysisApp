package per.demp;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class StockMain {

    int testTimeRange;
    int timeLimit;
    String shareRange;
    int valueDayInterval;
    String valueThreshold;
    int maRange1;
    int maRange2;
    int scopeLimit;

    private List<HistoricalQuote> stockHistory = new ArrayList<>();
    private int stockHistorySize = 0;
    private BigDecimal maRefMax;

    public StockMain() {
        this.testTimeRange = 10;
        this.timeLimit = 90;
        this.shareRange = "1.04";
        this.valueThreshold = "2000000000";
        this.valueDayInterval = 2;
        this.maRange1 = 20;
        this.maRange2 = 60;
        this.scopeLimit = timeLimit * 5;
    }

    public Map<String, List<TargetStock>> app(String[] symbols) throws IOException {
        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.DATE, -scopeLimit);
        Map<String, Stock> stockMap = YahooFinance.get(symbols, from, to, Interval.DAILY);
        List<Stock> stocks = new ArrayList<>(stockMap.values());
        Map<String, List<TargetStock>> targetStocksMap = new HashMap<>();
        for (Stock stock : stocks) {
            this.stockHistory = stock.getHistory();
            stockHistorySize = stockHistory.size();
            int t  = stockHistorySize - 1 - testTimeRange;
            int timeCounter = timeLimit;
            if (stockHistory.isEmpty() || stockHistorySize < maRange2) continue;
            List<TargetStock> targetStocks = new ArrayList<>();
            try {
                for (; timeCounter >= 0 && (t-maRange2) > 0; t--, timeCounter--) {
                    if (sharesTransactedFilter(t, new BigDecimal(valueThreshold), valueDayInterval)) continue;
                    if (closeFilter(t)) continue;
                    if (shareFilter(t, shareRange, maRange1)) continue;
                    TargetStock targetStock = new TargetStock(stock.getSymbol(), stockHistorySize - t - 1, false, stockHistory.get(t).getDate().getTime());
                    if (isRise(t)) {
                        targetStock.isMatch = true;
                    }
                    targetStocks.add(targetStock);
                    t -= 5;
                    timeCounter -= 5;
                }
                if (!targetStocks.isEmpty()) {
                    String symbol = targetStocks.get(0).symbol;
                    targetStocksMap.put(symbol, targetStocks);
                }
            }catch (Exception e){
                System.out.println("----------"+stock.getSymbol()+" : "+e.getMessage()+"----------");
            }
        }
        return targetStocksMap;
    }

    private boolean sharesTransactedFilter(int t, BigDecimal threshold, int dayInterval) {
        BigDecimal value = new BigDecimal(0);
        while (dayInterval >= 0) {
            long volume = stockHistory.get(t - dayInterval).getVolume();
            BigDecimal close = stockHistory.get(t - dayInterval).getClose();
            value = value.add(close.multiply(new BigDecimal(volume)));
            dayInterval--;
        }
        return value.compareTo(threshold) <= 0;
    }

    private BigDecimal getMa(int t, int ma) {
        BigDecimal output = new BigDecimal(0);
        for (int i = 0; i < ma; i++) {
            output = output.add(stockHistory.get(t - i).getClose());
        }
        output = output.divide(new BigDecimal(ma), 2, RoundingMode.HALF_UP);
        return output;
    }

    private boolean closeFilter(int t) {
        BigDecimal share = stockHistory.get(t).getClose();
        BigDecimal ma1 = getMa(t, maRange1);
        BigDecimal ma2 = getMa(t, maRange2);
        if (share.compareTo(ma1) < 0 && share.compareTo(ma1) < 0) return true;
        return ma1.compareTo(ma2) < 0;
    }

    private boolean shareFilter(int t, String range, int ma) {
        BigDecimal maRef = getMa(t, ma);
        maRefMax = maRef.multiply(new BigDecimal(range));
        BigDecimal shareLow = stockHistory.get(t).getLow();
        BigDecimal shareHigh = stockHistory.get(t).getHigh();
        //System.out.println(stockHistory.get(t).getSymbol());
        //System.out.println("t: "+ (stockHistorySize - t -1) + " Low: "+ shareLow + " High: " + shareHigh +" LowRef: " + maRef +" HighRef: " + maRefMax );
        return shareHigh.compareTo(maRef) < 0 || shareLow.compareTo(maRefMax) > 0;
    }

    private boolean isRise(int t0) {
        BigDecimal targetShare = maRefMax.multiply(new BigDecimal("1.08"));
        int t = t0 + 1;
        int counter = 0;
        for(; t < stockHistorySize && counter < testTimeRange; t++, counter++) {
            BigDecimal shareHigh = stockHistory.get(t).getHigh();
            if (shareHigh.compareTo(targetShare) >= 0) {
                return true;
            }
        }
        return false;
    }


    public void setTestTimeRange(int testTimeRange) {
        this.testTimeRange = testTimeRange;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setShareRange(String shareRange) {
        this.shareRange = shareRange;
    }

    public void setValueThreshold(String valueThreshold) {
        this.valueThreshold = valueThreshold;
    }

    public void setValueDayInterval(int valueDayInterval) {
        this.valueDayInterval = valueDayInterval;
    }

    public void setMaRange1(int maRange1) {
        this.maRange1 = maRange1;
    }

    public void setMaRange2(int maRange2) {
        this.maRange2 = maRange2;
    }
}


