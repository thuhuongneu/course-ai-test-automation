package com.jim.pos.core;

/**
 * Mot element trong cay accessibility (MSAA) cua app Flutter.
 *
 * <p>Luu y ve {@link #name}: voi o nhap lieu, {@code name} chinh la <b>placeholder</b> - no BIEN MAT
 * ngay khi o co noi dung hoac duoc focus. Vi vay khong duoc dung {@code name} lam locator cho o
 * nhap; hay dinh vi theo role + thu tu tren man hinh (xem {@link AxDriver#findByRole}).
 */
public class AxNode {

  public final int depth;
  public final String role;
  public final String name;
  public final String value;
  public final int left;
  public final int top;
  public final int width;
  public final int height;

  public AxNode(int depth, String role, String name, String value,
      int left, int top, int width, int height) {
    this.depth = depth;
    this.role = role;
    this.name = name;
    this.value = value;
    this.left = left;
    this.top = top;
    this.width = width;
    this.height = height;
  }

  @Override
  public String toString() {
    return String.format("%s name='%s' value='%s' rect=%d,%d %dx%d",
        role, name, value, left, top, width, height);
  }
}
