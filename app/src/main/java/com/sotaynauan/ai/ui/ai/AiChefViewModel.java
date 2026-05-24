package com.sotaynauan.ai.ui.ai;

import com.sotaynauan.ai.data.model.AiChefFeature;
import com.sotaynauan.ai.data.model.AiChefState;
import com.sotaynauan.ai.data.repository.AiChefRepository;

import java.util.List;

public class AiChefViewModel {
    private final AiChefRepository repository;

    public AiChefViewModel(AiChefRepository repository) {
        this.repository = repository;
    }

    public List<AiChefFeature> loadFeatures() {
        return repository.getFeatures();
    }

    public AiChefFeature loadFeature(String featureId) {
        return repository.findFeature(featureId);
    }

    public AiChefState loadState() {
        return repository.getState();
    }

    public AiChefState selectFeature(String featureId) {
        return repository.selectFeature(featureId);
    }
}
