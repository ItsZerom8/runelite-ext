/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.projectilewarnings;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Projectile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

public class ProjectileWarningsOverlay extends Overlay
{
	private static final int FILL_START_ALPHA = 25;
	private static final int OUTLINE_START_ALPHA = 255;

	private final Client client;
	private final ProjectileWarningsPlugin plugin;
	private final ProjectileWarningsConfig config;

	@Inject
	public ProjectileWarningsOverlay(Client client, ProjectileWarningsPlugin plugin, ProjectileWarningsConfig config)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enabled())
		{
			return null;
		}

		Instant now = Instant.now();
		Map<Projectile, AOEProjectile> projectiles = plugin.getProjectiles();
		for (Iterator<AOEProjectile> it = projectiles.values().iterator(); it.hasNext(); )
		{
			AOEProjectile aoeProjectile = it.next();

			if (now.isAfter(aoeProjectile.getStartTime().plus(aoeProjectile.getAoeProjectileInfo().getLifeTime())))
			{
				it.remove();
				continue;
			}

			Polygon tilePoly = Perspective.getCanvasTileAreaPoly(client, aoeProjectile.getTargetPoint(), aoeProjectile.getAoeProjectileInfo().getEffectSize());
			if (tilePoly == null)
			{
				continue;
			}

			// how far through the projectiles lifetime between 0-1.
			double progress = (System.currentTimeMillis() - aoeProjectile.getStartTime().toEpochMilli()) / (double) aoeProjectile.getAoeProjectileInfo().getLifeTime().toMillis();

			int fillAlpha = (int) ((1 - progress) * FILL_START_ALPHA);//alpha drop off over lifetime
			int outlineAlpha = (int) ((1 - progress) * OUTLINE_START_ALPHA);

			if (fillAlpha < 0)
			{
				fillAlpha = 0;
			}
			if (outlineAlpha < 0)
			{
				outlineAlpha = 0;
			}

			if (fillAlpha > 255)
			{
				fillAlpha = 255;
			}
			if (outlineAlpha > 255)
			{
				outlineAlpha = 255;//Make sure we don't pass in an invalid alpha
			}

			if (config.isOutlineEnabled())
			{
				graphics.setColor(new Color(255, 0, 0, outlineAlpha));
				graphics.drawPolygon(tilePoly);
			}

			graphics.setColor(new Color(255, 0, 0, fillAlpha));
			graphics.fillPolygon(tilePoly);
		}
		return null;
	}
}
