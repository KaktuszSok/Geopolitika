package kaktusz.geopolitika.integration;

public class XaeroIntegrationCommon {
	public static void postInit() {
		XaeroMinimapIntegration.postInit();
		XaeroWorldmapIntegration.postInit();
	}
}
