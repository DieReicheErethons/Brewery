package com.dre.brewery.recipe;

import java.util.concurrent.ThreadLocalRandom;

import com.dre.brewery.P;

public interface BUserError {
    public String userMessage();

    public class BadIngredientKindError implements BUserError {
        private Ingredient excessItemStack;

        public BadIngredientKindError(Ingredient of) {
            this.excessItemStack = of;
        }

        @Override
        public String userMessage() {
            int messagePermutation = ThreadLocalRandom.current().nextInt(1, 3+1);
            return P.p.languageReader.get("UserError_BadIngredientKind_"+messagePermutation, excessItemStack.displayName());
        }
    }
    public class MissingIngredientKindError implements BUserError {
        private RecipeItem missingItem;

        public MissingIngredientKindError(RecipeItem missingItem) {
            this.missingItem = missingItem;
        }

        @Override
        public String userMessage() {
            int messagePermutation = ThreadLocalRandom.current().nextInt(1, 3+1);
            return P.p.languageReader.get("UserError_MissingIngredientKind_"+messagePermutation, missingItem.displayName());
        }
    }
    public class IngredientQuantityError implements BUserError {
        private RecipeItem item;
        private int actualCount;

        public IngredientQuantityError(RecipeItem item, int actualCount) {
            this.item = item;
            this.actualCount = actualCount;
        }

        @Override
        public String userMessage() {
            int messagePermutation = ThreadLocalRandom.current().nextInt(1, 2+1);
            if (actualCount < item.getAmount()) {
                return P.p.languageReader.get("UserError_IngredientQuantity_TooLittle_" + messagePermutation, item.displayName());
            } else if (actualCount > item.getAmount()) {
                return P.p.languageReader.get("UserError_IngredientQuantity_TooMuch_" + messagePermutation, item.displayName());
            }

            return "";
        }
    }
    public class DistillationError implements BUserError {
        private boolean actuallyDistilled;
        private boolean neededDistillation;

        public DistillationError(boolean actuallyDistilled, boolean neededDistillation) {
            this.actuallyDistilled = actuallyDistilled;
            this.neededDistillation = neededDistillation;
        }

        @Override
        public String userMessage() {
            int messagePermutation = ThreadLocalRandom.current().nextInt(1, 2+1);

            if (actuallyDistilled && !neededDistillation) {
                return P.p.languageReader.get("UserError_DidntNeedDistillation_" + messagePermutation);
            } else if (neededDistillation && !actuallyDistilled) {
                return P.p.languageReader.get("UserError_NeededDistillation_" + messagePermutation);
            }

            return "";
        }
    }
}
