import arc.util.Align
import arc.util.Timer
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.entities.Units
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.mod.Plugin
import mindustry.type.Item
import mindustry.world.Block
import java.util.*


class CargoPods : Plugin() {

    var random = Random()
    lateinit var cargo: Block
    val spawnInterval: Float

    init {
        spawnInterval = getProp("spawnInterval").toFloat()
    }

    @Suppress("UNCHECKED_CAST")
    fun getProp(key: String): String {
        val props  = javaClass.classLoader.getResourceAsStream("config.properties").use {
            Properties().apply { load(it) }
        }
        return (props.getProperty(key)) ?: throw RuntimeException("Could not find property $key")
    }

    override fun init() {
        cargo = Blocks.vault
        Timer.schedule({
            if (Vars.state.isPlaying && !Vars.state.serverPaused) {
                spawnCargo(getPosition(), generateContent())
            }
        }, 0f, spawnInterval)
    }

    data class Position(val x: Int, val y: Int)

    private tailrec fun getPosition(): Position {
        val pos = generateRandomPosition()
        return if (isValidPosition(pos)) pos else getPosition()
    }

    private fun generateRandomPosition() = Position(random.nextInt(Vars.world.width()), random.nextInt(Vars.world.height()))

    private fun isValidPosition(pos: Position): Boolean {

        if ((cargo.solid || cargo.solidifes)
            && Units.anyEntities(
                pos.x * Vars.tilesize + cargo.offset - cargo.size * Vars.tilesize/2f,
                pos.y * Vars.tilesize + cargo.offset - cargo.size * Vars.tilesize/2f,
                (cargo.size * Vars.tilesize).toFloat(), (cargo.size * Vars.tilesize).toFloat()))
            return false

        val tile = Vars.world.tile(pos.x, pos.y) ?: return false;

        val offsetx = -(cargo.size - 1) / 2;
        val offsety = -(cargo.size - 1) / 2;

        for (dx in 0 until cargo.size) {
            for (dy in 0 until cargo.size){

                val wx = dx + offsetx + tile.x
                val wy = dy + offsety + tile.y

                val check = Vars.world.tile(wx, wy);

                if (check == null || (check.floor().isDeep) || check.solid() || check.build != null || !check.floor().placeableOn) return false;
            }
        }

        return true;
    }

    private fun generateContent(): Item {
        return Vars.content.items().random()
    }

    private fun spawnCargo(pos: Position, content: Item) {
        Call.setTile(Vars.world.tile(pos.x, pos.y), cargo, Team.derelict, 0)
        Call.effect(Fx.upgradeCore, (Vars.tilesize * pos.x).toFloat(), (Vars.tilesize * pos.y).toFloat(), cargo.size.toFloat(), Team.derelict.color)
        Call.infoPopup("Cargo Pod with [#${content.color}]${content.name}[] landed at ${pos.x}, ${pos.y}", 5f, Align.bottom, 0, 0, 0, 0)
        try {
            Call.setItem(Vars.world.build(pos.x, pos.y), content, cargo.itemCapacity)
        } catch (e: Exception) {}
    }
}