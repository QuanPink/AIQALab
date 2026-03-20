package com.framework.api.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents one position-statistics record returned by {@code searchPositionStatistic}.
 * Unknown fields are silently ignored to tolerate schema additions.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionStatisticsData {

    private String id;
    private String account;
    private String protocol;
    private String type;

    // ── Duration ────────────────────────────────────────────────────────
    private Long maxDuration;
    private Long minDuration;
    private Double avgDuration;

    // ── Trade counts ────────────────────────────────────────────────────
    private Integer totalTrade;
    private Integer totalWin;
    private Integer totalLoss;
    private Integer totalLiquidation;
    private Double totalLiquidationAmount;

    // ── Volume & fee ────────────────────────────────────────────────────
    private Double avgVolume;
    private Double totalVolume;
    private Double totalLongVolume;
    private Double totalShortVolume;
    private Double totalFee;

    // ── Ratios ──────────────────────────────────────────────────────────
    private Double winRate;
    private Double longRate;
    private Double profitRate;
    private Double gainLossRatio;
    private Double profitLossRatio;
    private Double orderPositionRatio;

    // ── PnL (unrealised + realised) ─────────────────────────────────────
    private Double pnl;
    private Double unrealisedPnl;
    private Double longPnl;
    private Double shortPnl;
    private Double realisedPnl;
    private Double realisedLongPnl;
    private Double realisedShortPnl;
    private Double realisedTotalGain;
    private Double realisedTotalLoss;
    private Double totalGain;

    // ── ROI ─────────────────────────────────────────────────────────────
    private Double avgRoi;
    private Double maxRoi;
    private Double maxPnl;
    private Double realisedAvgRoi;
    private Double realisedMaxRoi;
    private Double realisedMaxPnl;

    // ── Leverage ─────────────────────────────────────────────────────────
    private Double avgLeverage;
    private Double maxLeverage;
    private Double minLeverage;

    // ── Drawdown ─────────────────────────────────────────────────────────
    private Double maxDrawdown;
    private Double maxDrawdownPnl;
    private Double realisedMaxDrawdown;
    private Double realisedMaxDrawdownPnl;

    // ── Risk metrics ─────────────────────────────────────────────────────
    private Double sharpeRatio;
    private Double sortinoRatio;
    private Double realisedSharpeRatio;
    private Double realisedSortinoRatio;
    private Double realisedProfitRate;
    private Double realisedGainLossRatio;
    private Double realisedProfitLossRatio;

    // ── Streaks ───────────────────────────────────────────────────────────
    private Integer winStreak;
    private Integer loseStreak;
    private Integer maxWinStreak;
    private Integer maxLoseStreak;

    // ── Meta ──────────────────────────────────────────────────────────────
    private Integer runTimeDays;
    private List<String> indexTokens;
    private List<String> pairs;
    private String lastTradeAt;
    private Long lastTradeAtTs;
    private String statisticAt;
    private String createdAt;
    private String updatedAt;
    private List<String> ifLabels;
    private List<String> ifGoodMarkets;
    private List<String> ifBadMarkets;
    private List<String> statisticLabels;
    private List<String> aggregatedLabels;
    private List<String> realisedStatisticLabels;
    private List<String> realisedAggregatedLabels;
}
