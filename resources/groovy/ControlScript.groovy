import gate.termraider.*;

// equivalent to old preprocessor
eachDocument {
    docReset();
    languageIdentifier();
    tokenizer();
    gazetteer();
    bwpGazetteer();
    senSpliter();
    posTagger();
    morphAnalyzer();
    stopWordMarker();
    simpleNounChunker();
    neExtracter();
    vpChunker();
    orthoMatcher();
    selectTokens();
    multiwordJape();
    deduplicateMW();
    augmentation();
    tfidfCopier();
}

// create the TfIdfTermbank LR
def termbank0 = Factory.createResource("gate.termraider.bank.TfIdfTermbank", [
  corpora:[corpus],
  inputAnnotationTypes:'SingleWord;MultiWord',
  debugMode:true,
].toFeatureMap())

eachDocument {
    tfIdfCopier(termbank:termbank0);
    augmentation();
}

// create the AugmentedTermbank LR

def termbank1 = Factory.createResource("gate.termraider.bank.AnnotationTermbank", [
  corpora:[corpus],
  inputAnnotationTypes:'SingleWord;MultiWord',
  debugMode:true,
  inputScoreFeature:'localAugTfIdf'
].toFeatureMap())

// create the HyponymyTermbank LR
def termbank2 = Factory.createResource("gate.termraider.bank.HyponymyTermbank", [
  corpora:[corpus],
  inputAnnotationTypes:'SingleWord;MultiWord',
  debugMode:true,
  inputHeadFeatures:'head;canonical',
  inputAnnotationFeature:'canonical'
].toFeatureMap())


eachDocument {
    augTfIdfCopier(termbank:termbank1);
    kyotoCopier(termbank:termbank2);
}
