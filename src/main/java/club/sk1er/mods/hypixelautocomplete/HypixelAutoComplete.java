package club.sk1er.mods.hypixelautocomplete;

//#if FORGE
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.client.ConfigScreenHandler;
//$$ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#if MC==11902
//$$ import net.minecraftforge.fml.ModLoadingContext;
//#endif
//$$ @Mod(HypixelAutoComplete.MOD_ID)
public class HypixelAutoComplete {
    public static final String MOD_ID = "hypixel_auto_complete";
    public static final String MOD_VERSION = "2.0";
    public static final String MOD_NAME = "Hypixel Autocomplete";

    //#if FORGE
    //$$ public HypixelAutoComplete(FMLJavaModLoadingContext context) {
    //#if MC==11902
    //$$     ModLoadingContext.get().registerExtensionPoint(
    //#else
    //$$     context.registerExtensionPoint(
    //#endif
    //$$         ConfigScreenHandler.ConfigScreenFactory.class,
    //$$         () -> new ConfigScreenHandler.ConfigScreenFactory(
    //$$             (minecraft, screen) -> Config.INSTANCE.gui()
    //$$         )
    //$$     );
    //$$ }
}
