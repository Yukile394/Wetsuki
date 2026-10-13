package dev.wetsuki.util.render.providers;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
	public static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey RECTANGLE_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("rectangle"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey RECTANGLE_BORDER_SHADER_KEY = new ShaderProgramKey(getShaderIdentifier("border"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
	public static final ShaderProgramKey GLASS_SHADER_KEY = new ShaderProgramKey(getGlass("data"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

	public static final Identifier firefly = Identifier.of("wetsukiclient", "images/particles/firefly.png");
	public static final Identifier bloom = Identifier.of("wetsukiclient", "images/particles/bloom.png");
	public static final Identifier snowflake = Identifier.of("wetsukiclient", "images/particles/snowflake.png");
	public static final Identifier dollar = Identifier.of("wetsukiclient", "images/particles/dollar.png");
	public static final Identifier heart = Identifier.of("wetsukiclient", "images/particles/heart.png");
	public static final Identifier star = Identifier.of("wetsukiclient", "images/particles/star.png");
	public static final Identifier spark = Identifier.of("wetsukiclient", "images/particles/spark.png");
	public static final Identifier crown = Identifier.of("wetsukiclient", "images/particles/crown.png");
	public static final Identifier lightning = Identifier.of("wetsukiclient", "images/particles/lightning.png");
	public static final Identifier line = Identifier.of("wetsukiclient", "images/particles/line.png");
	public static final Identifier point = Identifier.of("wetsukiclient", "images/particles/point.png");
	public static final Identifier rhombus = Identifier.of("wetsukiclient", "images/particles/rhombus.png");


	public static final Identifier marker = Identifier.of("wetsukiclient", "images/targetesp/target.png");
	public static final Identifier marker2 = Identifier.of("wetsukiclient", "images/targetesp/target2.png");


	public static final Identifier CUSTOM_CAPE = Identifier.of("wetsukiclient", "cape/cape.png");
	public static final Identifier CUSTOM_ELYTRA = Identifier.of("wetsukiclient", "cape/elytra.png");

	public static final Identifier container = Identifier.of("wetsukiclient", "images/hud/container.png");

	public static final Identifier color_image = Identifier.of("wetsukiclient", "images/gui/pick.png");


	private static Identifier getGlass(String name) {
		return Identifier.of("wetsukiclient", "core/glass/" + name);
	}
	private static Identifier getShaderIdentifier(String name) {
		return Identifier.of("wetsukiclient", "core/" + name);
	}
}