package com.mediarenamer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SeasonDirectoryMatcher 单元测试
 *
 * 测试季目录特征匹配功能：
 * - 标准英文格式 (Season 01, Season 1)
 * - 缩写英文格式 (S1, S02)
 * - 中文格式 (第1季, 第一季)
 * - 特别篇格式 (Specials, Special)
 * - 非季目录的识别
 */
class SeasonDirectoryMatcherTest {

    @Test
    void testParseSeasonFolder_标准英文格式() {
        // 测试标准 "Season ##" 格式
        SeasonDirectoryMatcher.SeasonInfo season1 = SeasonDirectoryMatcher.parseSeasonFolder("Season 01");
        assertNotNull(season1);
        assertEquals(1, season1.getSeasonNumber());
        assertEquals("STANDARD_EN", season1.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo season2 = SeasonDirectoryMatcher.parseSeasonFolder("Season 2");
        assertNotNull(season2);
        assertEquals(2, season2.getSeasonNumber());

        // 测试大小写不敏感
        SeasonDirectoryMatcher.SeasonInfo seasonUpper = SeasonDirectoryMatcher.parseSeasonFolder("SEASON 10");
        assertNotNull(seasonUpper);
        assertEquals(10, seasonUpper.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo seasonLower = SeasonDirectoryMatcher.parseSeasonFolder("season 05");
        assertNotNull(seasonLower);
        assertEquals(5, seasonLower.getSeasonNumber());
    }

    @Test
    void testParseSeasonFolder_缩写英文格式() {
        // 测试 "S##" 格式
        SeasonDirectoryMatcher.SeasonInfo s1 = SeasonDirectoryMatcher.parseSeasonFolder("S1");
        assertNotNull(s1);
        assertEquals(1, s1.getSeasonNumber());
        assertEquals("SHORT_EN", s1.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo s02 = SeasonDirectoryMatcher.parseSeasonFolder("S02");
        assertNotNull(s02);
        assertEquals(2, s02.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo s10 = SeasonDirectoryMatcher.parseSeasonFolder("S10");
        assertNotNull(s10);
        assertEquals(10, s10.getSeasonNumber());

        // 测试大小写不敏感
        SeasonDirectoryMatcher.SeasonInfo sLower = SeasonDirectoryMatcher.parseSeasonFolder("s05");
        assertNotNull(sLower);
        assertEquals(5, sLower.getSeasonNumber());
    }

    @Test
    void testParseSeasonFolder_中文格式_阿拉伯数字() {
        // 测试中文格式 + 阿拉伯数字
        SeasonDirectoryMatcher.SeasonInfo season1 = SeasonDirectoryMatcher.parseSeasonFolder("第1季");
        assertNotNull(season1);
        assertEquals(1, season1.getSeasonNumber());
        assertEquals("CHINESE", season1.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo season2 = SeasonDirectoryMatcher.parseSeasonFolder("第2季");
        assertNotNull(season2);
        assertEquals(2, season2.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo season10 = SeasonDirectoryMatcher.parseSeasonFolder("第10季");
        assertNotNull(season10);
        assertEquals(10, season10.getSeasonNumber());

        // 测试带空格的情况
        SeasonDirectoryMatcher.SeasonInfo seasonWithSpace = SeasonDirectoryMatcher.parseSeasonFolder("第 5 季");
        assertNotNull(seasonWithSpace);
        assertEquals(5, seasonWithSpace.getSeasonNumber());
    }

    @Test
    void testParseSeasonFolder_中文格式_中文数字() {
        // 测试中文格式 + 中文数字
        SeasonDirectoryMatcher.SeasonInfo season1 = SeasonDirectoryMatcher.parseSeasonFolder("第一季");
        assertNotNull(season1);
        assertEquals(1, season1.getSeasonNumber());
        assertEquals("CHINESE", season1.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo season2 = SeasonDirectoryMatcher.parseSeasonFolder("第二季");
        assertNotNull(season2);
        assertEquals(2, season2.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo season3 = SeasonDirectoryMatcher.parseSeasonFolder("第三季");
        assertNotNull(season3);
        assertEquals(3, season3.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo season10 = SeasonDirectoryMatcher.parseSeasonFolder("第十季");
        assertNotNull(season10);
        assertEquals(10, season10.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo season20 = SeasonDirectoryMatcher.parseSeasonFolder("第二十季");
        assertNotNull(season20);
        assertEquals(20, season20.getSeasonNumber());
    }

    @Test
    void testParseSeasonFolder_特别篇格式() {
        // 测试 Specials 格式
        SeasonDirectoryMatcher.SeasonInfo specials1 = SeasonDirectoryMatcher.parseSeasonFolder("Specials");
        assertNotNull(specials1);
        assertEquals(0, specials1.getSeasonNumber());
        assertEquals("SPECIALS", specials1.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo specials2 = SeasonDirectoryMatcher.parseSeasonFolder("Special");
        assertNotNull(specials2);
        assertEquals(0, specials2.getSeasonNumber());

        // 测试大小写不敏感
        SeasonDirectoryMatcher.SeasonInfo specialsUpper = SeasonDirectoryMatcher.parseSeasonFolder("SPECIALS");
        assertNotNull(specialsUpper);
        assertEquals(0, specialsUpper.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo specialsLower = SeasonDirectoryMatcher.parseSeasonFolder("special");
        assertNotNull(specialsLower);
        assertEquals(0, specialsLower.getSeasonNumber());
    }

    @Test
    void testParseSeasonFolder_非季目录() {
        // 测试普通目录名（不应该匹配）
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("小猪佩奇"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("Breaking Bad"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("Game of Thrones"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("武林外传"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("琅琊榜"));

        // 测试包含季信息但格式不对的目录名
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("Season 1 - 第一季"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("S01E01"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("第一季合集"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("Specials Bonus"));
    }

    @Test
    void testParseSeasonFolder_边界情况() {
        // 测试空值和空字符串
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder(null));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder(""));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("   "));

        // 测试仅包含数字的目录名（不应该匹配）
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("01"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("1"));
        assertNull(SeasonDirectoryMatcher.parseSeasonFolder("10"));

        // 测试带前导/尾随空格
        SeasonDirectoryMatcher.SeasonInfo withSpace = SeasonDirectoryMatcher.parseSeasonFolder("  Season 01  ");
        assertNotNull(withSpace);
        assertEquals(1, withSpace.getSeasonNumber());
    }

    @Test
    void testIsSeasonFolder_便捷方法() {
        // 测试便捷方法
        assertTrue(SeasonDirectoryMatcher.isSeasonFolder("Season 01"));
        assertTrue(SeasonDirectoryMatcher.isSeasonFolder("S1"));
        assertTrue(SeasonDirectoryMatcher.isSeasonFolder("第1季"));
        assertTrue(SeasonDirectoryMatcher.isSeasonFolder("Specials"));

        assertFalse(SeasonDirectoryMatcher.isSeasonFolder("Breaking Bad"));
        assertFalse(SeasonDirectoryMatcher.isSeasonFolder(null));
        assertFalse(SeasonDirectoryMatcher.isSeasonFolder(""));
    }

    @Test
    void testParseSeasonFolder_覆盖所有匹配类型() {
        // 确保所有匹配类型都被正确识别
        SeasonDirectoryMatcher.SeasonInfo standard = SeasonDirectoryMatcher.parseSeasonFolder("Season 05");
        assertEquals("STANDARD_EN", standard.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo shortEn = SeasonDirectoryMatcher.parseSeasonFolder("S3");
        assertEquals("SHORT_EN", shortEn.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo specials = SeasonDirectoryMatcher.parseSeasonFolder("Specials");
        assertEquals("SPECIALS", specials.getMatchType());

        SeasonDirectoryMatcher.SeasonInfo chinese = SeasonDirectoryMatcher.parseSeasonFolder("第2季");
        assertEquals("CHINESE", chinese.getMatchType());
    }

    @Test
    void testSeasonInfo_toString() {
        // 测试 SeasonInfo 的 toString 方法
        SeasonDirectoryMatcher.SeasonInfo season = SeasonDirectoryMatcher.parseSeasonFolder("Season 01");
        String toString = season.toString();

        assertTrue(toString.contains("Season 1"));
        assertTrue(toString.contains("Season 01"));
        assertTrue(toString.contains("STANDARD_EN"));
    }

    @Test
    void testParseSeasonFolder_大量季数() {
        // 测试大季数（如果有超长剧集）
        SeasonDirectoryMatcher.SeasonInfo season99 = SeasonDirectoryMatcher.parseSeasonFolder("Season 99");
        assertNotNull(season99);
        assertEquals(99, season99.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo s100 = SeasonDirectoryMatcher.parseSeasonFolder("S100");
        assertNotNull(s100);
        assertEquals(100, s100.getSeasonNumber());
    }

    @Test
    void testParseSeasonFolder_前导零处理() {
        // 测试前导零的正确处理
        SeasonDirectoryMatcher.SeasonInfo s01 = SeasonDirectoryMatcher.parseSeasonFolder("Season 01");
        SeasonDirectoryMatcher.SeasonInfo s1 = SeasonDirectoryMatcher.parseSeasonFolder("Season 1");

        assertEquals(s01.getSeasonNumber(), s1.getSeasonNumber());
        assertEquals(1, s01.getSeasonNumber());

        SeasonDirectoryMatcher.SeasonInfo s001 = SeasonDirectoryMatcher.parseSeasonFolder("S001");
        assertNotNull(s001);
        assertEquals(1, s001.getSeasonNumber());
    }
}
