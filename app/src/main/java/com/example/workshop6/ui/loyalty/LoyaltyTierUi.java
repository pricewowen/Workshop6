// Contributor(s): Owen
// Main: Owen - Tier badge styling helpers for Me and orders.

package com.example.workshop6.ui.loyalty;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.RewardTierDto;

import java.text.NumberFormat;
import java.util.List;

/**
 * Shared loyalty tier resolution and view binding for Me, orders and rewards surfaces.
 */
public final class LoyaltyTierUi {

    private LoyaltyTierUi() {
    }

    /**
     * Resolves the tier from reward tier definitions (point windows and discount percent).
     * Prefers the tier whose {@code minPoints} and {@code maxPoints} contain the balance. Otherwise
     * uses {@code rewardTierId} from the customer payload, then the highest tier with
     * {@code minPoints <= points}.
     */
    public static RewardTierDto resolveCurrentTier(List<RewardTierDto> rewardTiers, int points, Integer assignedTierId) {
        if (rewardTiers == null || rewardTiers.isEmpty()) {
            return null;
        }
        for (RewardTierDto t : rewardTiers) {
            int max = t.maxPoints != null ? t.maxPoints : Integer.MAX_VALUE;
            if (points >= t.minPoints && points <= max) {
                return t;
            }
        }
        if (assignedTierId != null) {
            for (RewardTierDto t : rewardTiers) {
                if (t.id != null && t.id.equals(assignedTierId)) {
                    return t;
                }
            }
        }
        RewardTierDto best = null;
        for (RewardTierDto t : rewardTiers) {
            if (points >= t.minPoints && (best == null || t.minPoints > best.minPoints)) {
                best = t;
            }
        }
        return best;
    }

    public static String buildTierMilestoneDescription(Context ctx, NumberFormat nf, RewardTierDto t) {
        double pct = 0d;
        if (t.discountRatePercent != null) {
            pct = t.discountRatePercent.doubleValue();
        }
        String minStr = nf.format(t.minPoints);
        if (t.maxPoints != null) {
            String maxStr = nf.format(t.maxPoints);
            return ctx.getString(R.string.loyalty_tier_desc_closed_range, pct, minStr, maxStr);
        }
        return ctx.getString(R.string.loyalty_tier_desc_open_range, pct, minStr);
    }

    /** Points to deduct for one redemption. Scales with tier discount percent into point cost. */
    public static int redeemPointsCost(RewardTierDto tier) {
        if (tier == null || tier.discountRatePercent == null) {
            return 0;
        }
        double pct = tier.discountRatePercent.doubleValue();
        if (pct <= 0) {
            return 0;
        }
        return (int) Math.round(pct * 100);
    }

    /** Cart discount as a fraction where 0.05 means five percent off list price. */
    public static double redeemDiscountFraction(RewardTierDto tier) {
        if (tier == null || tier.discountRatePercent == null) {
            return 0d;
        }
        return tier.discountRatePercent.doubleValue() / 100.0;
    }

    /**
     * Fills loyalty summary widgets from {@link #resolveCurrentTier} plus optional next tier progress.
     */
    public static void bindTierCard(Context ctx, NumberFormat nf, List<RewardTierDto> rewardTiers,
                                    int points, Integer assignedTierId,
                                    TextView tvPoints, TextView tvLevel, TextView tvTierDescription,
                                    TextView tvNextTier, TextView tvPointsNeeded, ProgressBar progressLoyalty) {
        RewardTierDto current = resolveCurrentTier(rewardTiers, points, assignedTierId);
        RewardTierDto next = null;
        if (current != null && rewardTiers != null) {
            int idx = -1;
            for (int i = 0; i < rewardTiers.size(); i++) {
                if (rewardTiers.get(i) == current) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0 && idx + 1 < rewardTiers.size()) {
                next = rewardTiers.get(idx + 1);
            }
        }

        if (current != null) {
            tvPoints.setText(nf.format(points));
            tvLevel.setText(current.name != null ? current.name : "");
            tvTierDescription.setText(buildTierMilestoneDescription(ctx, nf, current));
            if (next != null) {
                int pointsNeeded = Math.max(0, next.minPoints - points);
                String milestonePts = nf.format(next.minPoints);
                tvNextTier.setText(ctx.getString(R.string.label_next_tier_fmt, next.name, milestonePts));
                tvPointsNeeded.setText(ctx.getString(
                        R.string.label_points_needed_fmt,
                        nf.format(pointsNeeded),
                        next.name));
                int rangeStart = current.minPoints;
                int rangeEnd = next.minPoints;
                int rangeSize = Math.max(1, rangeEnd - rangeStart);
                int progress = ((points - rangeStart) * 100) / rangeSize;
                progressLoyalty.setProgress(Math.max(0, Math.min(100, progress)));
            } else {
                tvNextTier.setText(R.string.label_top_tier_reached);
                tvPointsNeeded.setText(R.string.label_highest_tier_reached);
                progressLoyalty.setProgress(100);
            }
        } else {
            tvPoints.setText(nf.format(points));
            tvLevel.setText(R.string.label_unknown_tier);
            tvTierDescription.setText(R.string.label_unknown_tier_desc);
            tvNextTier.setText(R.string.label_next_tier_na);
            tvPointsNeeded.setText(R.string.label_points_to_next_na);
            progressLoyalty.setProgress(0);
        }
    }
}
