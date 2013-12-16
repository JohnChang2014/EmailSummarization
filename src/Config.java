public class Config {
	public static final String ip = "localhost";
	public static final String port = "3306";
	public static final String db = "nlp";
	public static final String username = "root";
	public static final String password = "";
	public static final String plugins_home = "./plugins";
	public static final String ds_dir = "/data/clusters"; 
	public static final double threshold = 999999999.005;
	public static final String ieScriptController = "/resources/groovy/IEScriptController.groovy";
	public static final String dataMaintainScriptContoller = "/resources/groovy/DataMaintainScriptController.groovy";
	public static final String ifScriptController = "/resources/groovy/InferenceScriptController.groovy";
	
	// for data preprocessing
	public static final String dataset_root_path = "./data/";
	public static final String testset_root_path = "./evaluation/";
	public static final String storage_path1 = "raw/";
	public static final String storage_path2 = "content/";
	public static final String datafile_path  = "pdf/";
}
