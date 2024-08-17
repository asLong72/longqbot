package site.longint.controller.impl;

import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities;
import net.mamoe.mirai.event.Event;
import site.longint.configs.ComposeMemeConfig;
import site.longint.controller.Controller;

public class ComposeMemeController extends Controller {
    public static final ComposeMemeController INSTANCE = new ComposeMemeController();

    public ComposeMemeController() {
        super("合成", false);
    }

    @Override
    protected void register() {

    }

    @Override
    public void onCall(Event event, String[] arg) {

    }

    @Override
    public void info(Event event, String[] args) {

    }
}
